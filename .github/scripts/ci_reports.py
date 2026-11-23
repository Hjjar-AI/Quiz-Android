"""Summarize the XML reports produced by the selected hosted Gradle task."""

from collections import Counter
import xml.etree.ElementTree as ET


JVM_COLUMNS = ('tests', 'failures', 'errors', 'skipped')
LINT_COLUMNS = ('fatal', 'error', 'warning', 'information', 'other')


def jvm_counts(paths):
    counts = Counter()
    for path in paths:
        root = ET.parse(path).getroot()
        if root.tag != 'testsuite':
            raise ValueError('Unexpected JUnit report format')
        values = {key: int(root.attrib[key]) for key in JVM_COLUMNS}
        if min(values.values()) < 0 or sum(values[key] for key in JVM_COLUMNS[1:]) > values['tests']:
            raise ValueError('Invalid JUnit counts')
        counts.update(values)
    return counts


def lint_counts(path):
    root = ET.parse(path).getroot()
    if root.tag != 'issues':
        raise ValueError('Unexpected lint report format')
    counts = Counter()
    for issue in root.findall('issue'):
        severity = issue.attrib['severity'].lower()
        if severity == 'informational':
            severity = 'information'
        counts[severity if severity in LINT_COLUMNS else 'other'] += 1
    return counts


def report_summary(directory, modules, operation):
    lint = operation == 'lint'
    columns = LINT_COLUMNS if lint else JVM_COLUMNS
    labels = ('Fatal', 'Errors', 'Warnings', 'Information', 'Other') if lint else (
        'Cases', 'Failures', 'Errors', 'Skipped',
    )
    header = ' | '.join(('Module', *labels, 'Report'))
    lines = ['', '### Module lint findings' if lint else '### Module JVM results', '',
             f'| {header} |', '| ' + ' | '.join(['---'] * (len(labels) + 2)) + ' |']
    complete = True
    for module in modules:
        root = directory / module
        try:
            if lint:
                counts = lint_counts(root / 'reports/lint-results-debug.xml')
            else:
                reports = sorted((root / 'test-results/testDebugUnitTest').glob('TEST-*.xml'))
                if not reports:
                    raise FileNotFoundError
                counts = jvm_counts(reports)
        except FileNotFoundError:
            counts = None
            status = 'Missing'
        except (ET.ParseError, KeyError, OSError, ValueError):
            counts = None
            status = 'Unreadable or invalid'
        else:
            if lint:
                status = 'Errors reported' if counts['fatal'] or counts['error'] else 'Present'
            elif counts['tests'] <= counts['skipped']:
                status = 'No executed cases'
            elif counts['failures'] or counts['errors']:
                status = 'Failures reported'
            else:
                status = 'Present'
        complete &= status == 'Present'
        values = [str(counts[key]) if counts is not None else '—' for key in columns]
        lines.append('| ' + ' | '.join((module, *values, status)) + ' |')
    return '\n'.join((*lines, '')), complete
