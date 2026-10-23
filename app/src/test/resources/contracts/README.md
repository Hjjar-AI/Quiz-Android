# Android wire fixtures

Synthetic current-contract payloads, with no real accounts or credentials. Root entries are API `data` objects, not envelopes. Tests feed them through production ApiClient decoding and assert non-default values, fractions, explicit nulls, Boolean denial and exact comma-containing names.

Source of field shapes:

- analytics/mastery/streak: backend `apps/analytics/views.py` and `services/member.py`, activity service; Android `AnalyticsModels.kt`.
- users/active_accounts/permissions/roles/settings: backend `apps/users/serializers.py`, `views/admin_user_views.py`, `permission_views.py`; admin list augments UserSerializer with latest-exam fields. Active-account IDs are JSON object keys.
- flags: backend `apps/feedback/views.py::AdminFlagListView`; author identity is distinct from reporter identity.
- groups/leaderboard: backend `apps/groups/views.py`, `serializers.py`, `services.py`; member visibility is caller-specific and accuracy is fractional.
- planner: backend `apps/planning/serializers.py::StudyPlannerSerializer`; dates map to integer counts and tags remain exact names.

These reviewed fixtures do not claim automatic backend fixture generation or live cross-process verification. Backend suites separately exercise their serializers/endpoints. Update fixtures/assertions together only after checking a changed production contract.
