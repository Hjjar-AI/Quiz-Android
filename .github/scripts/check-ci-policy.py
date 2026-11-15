"""Source-only checks of manual hosted workflow safeguards; no workflow execution."""

from pathlib import Path
import re
import sys

from ci_yaml import load_yaml

ROOT = Path(__file__).resolve().parents[2]
PIN = re.compile(r'[\w.-]+/[\w.-]+(?:/[\w./-]+)?@[0-9a-f]{40}')
PUBLIC = '${{ github.event.repository.private == false }}'
CONFIRMED = "${{ inputs.confirm && github.event.repository.private == false && github.ref == format('refs/heads/{0}', github.event.repository.default_branch) }}"


def main():
    errors = []
    def need(value, message):
        if not value:
            errors.append(message)

    def steps(items, context, stack=()):
        need(isinstance(items, list) and bool(items), f'{context}: steps required')
        for step in items:
            if 'run' in step:
                need('${{' not in step['run'], f'{context}: pass expressions through env')
                continue
            action = step['uses']
            inputs = step.get('with', {})
            if action.startswith('./'):
                directory = (ROOT / action).resolve()
                need(directory.is_relative_to(ROOT / '.github/actions') and directory not in stack,
                     f'{context}: local action path/recursion')
                if not directory.is_relative_to(ROOT / '.github/actions') or directory in stack:
                    continue
                local = load_yaml(directory / 'action.yml')
                need(local['runs']['using'] == 'composite', f'{context}: composite action required')
                steps(local['runs']['steps'], action, (*stack, directory))
                continue
            need(PIN.fullmatch(action), f'{context}: full commit action pin required')
            name = action.split('@')[0]
            if name == 'actions/checkout':
                need(inputs.get('persist-credentials') is False, f'{context}: persisted credentials prohibited')
            if name == 'actions/upload-artifact':
                need(type(inputs.get('retention-days')) is int and inputs['retention-days'] == 1,
                     f'{context}: one-day retention required')
            if name == 'gradle/actions/setup-gradle':
                need(inputs.get('cache-provider') == 'basic', f'{context}: basic cache required')
                need(inputs.get('cache-read-only') == '${{ github.ref_name != github.event.repository.default_branch }}',
                     f'{context}: cache writes restricted to default branch')

    paths = sorted((ROOT / '.github/workflows').glob('*.yml'))
    for path in paths:
        data = load_yaml(path)
        name = path.name
        need(set(data['on']) == {'workflow_dispatch'}, f'{name}: only manual dispatch allowed')
        need(data['permissions'] == {'contents': 'read'}, f'{name}: read-only workflow token required')
        need(data['concurrency']['cancel-in-progress'] is False, f'{name}: non-cancelling concurrency required')
        if name != 'repository.yml':
            confirm = data['on']['workflow_dispatch']['inputs']['confirm']
            need(confirm['type'] == 'boolean' and confirm['default'] is False,
                 f'{name}: explicit opt-in default false required')
        for job in data['jobs'].values():
            need(job['if'] == (PUBLIC if name == 'repository.yml' else CONFIRMED),
                 f'{name}: public/default-branch/confirmation guard required')
            need(job['runs-on'] == 'ubuntu-24.04', f'{name}: standard Ubuntu runner required')
            need(type(job['timeout-minutes']) is int and 0 < job['timeout-minutes'] <= 60,
                 f'{name}: bounded timeout required')
            if 'permissions' in job:
                need(name == 'security.yml' and job['permissions'] == {'contents': 'read', 'security-events': 'write'},
                     f'{name}: unexpected job permissions')
            if any(step.get('uses') == './.github/actions/setup-android' for step in job['steps']):
                need(data['concurrency']['group'] == 'mukhtabir-hosted-android',
                     f'{name}: serialize hosted Android work across workflows')
            steps(job['steps'], name)
    for error in errors:
        print(error)
    print(f'Workflow policy: {len(paths)} manual workflows reviewed; {len(errors)} problems. Source checks only.')
    return 1 if errors else 0


if __name__ == '__main__':
    sys.exit(main())
