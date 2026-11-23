package com.mukhtabir.android.data.remote

import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

class PlannerService(private val api: ApiClient) : LearnerPlanner {
    override suspend fun recordProgress() { api.post(ApiEndpoints.Planner.PROGRESS) }

    override suspend fun get(): PlannerDto =
        api.decode(PlannerDto.serializer(), api.get(ApiEndpoints.Planner.GET))

    override suspend fun update(request: PlannerWriteRequest): PlannerDto = api.decode(PlannerDto.serializer(),
        api.post(ApiEndpoints.Planner.UPDATE, api.encode(PlannerWriteRequest.serializer(), request)))

    override suspend fun delete(id: Long, version: Int) { api.delete(ApiEndpoints.Planner.DELETE+"?expected_id=$id&expected_version=$version") }
}

class GroupService(private val api: ApiClient) {
    // This endpoint returns all active groups; search/paging are local, not API filters.
    suspend fun adminList(): GroupListDto =
        api.decode(GroupListDto.serializer(), api.get(ApiEndpoints.Groups.ADMIN_LIST))

    suspend fun mine(): GroupListDto =
        api.decode(GroupListDto.serializer(), api.get(ApiEndpoints.Groups.MINE))

    suspend fun leaderboard(groupId: Long, days: Int = 7): GroupLeaderboardDto = api.decode(
        GroupLeaderboardDto.serializer(),
        api.get(ApiEndpoints.Groups.leaderboard(groupId), mapOf("days" to days.toString())),
    )

    suspend fun setVisibility(groupId: Long, visible: Boolean) {
        api.post(
            ApiEndpoints.Groups.visibility(groupId),
            api.encode(GroupVisibilityRequest.serializer(), GroupVisibilityRequest(visible)),
        )
    }
}

class AdminService(private val api: ApiClient) {
    suspend fun user(id: Long): UserDto = api.decode(UserDto.serializer(),api.get(ApiEndpoints.Admin.user(id)))

    suspend fun activeAccounts(): ActiveAccountsDto = api.decode(
        ActiveAccountsDto.serializer(),api.get(ApiEndpoints.Admin.ACTIVE_USERS))

    suspend fun setUserActive(id: Long, adminPassword: String, active: Boolean) {
        val response=api.post(ApiEndpoints.Admin.setUserActive(id),buildJsonObject { put("admin_password",adminPassword); put("is_active",active) }) as? JsonObject
        if((response?.get("id") as? JsonPrimitive)?.longOrNull!=id || (response.get("is_active") as? JsonPrimitive)?.booleanOrNull!=active)
            throw ApiException(null, "") // Unknown write: reconcile, never claim the requested state was accepted.
    }

    suspend fun resetPassword(id: Long, adminPassword: String, newPassword: String) {
        // This native flow always supplies a password; never generate and discard a secret response.
        require(newPassword.isNotBlank())
        api.post(ApiEndpoints.Admin.resetUserPassword(id),buildJsonObject {
            put("admin_password",adminPassword)
            put("new_password",newPassword)
        })
    }

    suspend fun generatePassword(id: Long, adminPassword: String): GeneratedPasswordDto = api.decode(
        GeneratedPasswordDto.serializer(), api.post(ApiEndpoints.Admin.resetUserPassword(id),
            buildJsonObject { put("admin_password",adminPassword) }))

    suspend fun deleteUser(id: Long, adminPassword: String) {
        // Production Django supports no transfer/cascade deletion options.
        api.post(ApiEndpoints.Admin.deleteUser(id),buildJsonObject { put("admin_password",adminPassword) })
    }

    suspend fun users(
        page: Int = 1,
        perPage: Int = 20,
        search: String? = null,
    ): UserPageDto = api.decode(
        UserPageDto.serializer(),
        api.get(
            ApiEndpoints.Admin.USERS,
            mapOf("page" to page.toString(), "per_page" to perPage.toString(), "search" to search),
        ),
    )

    suspend fun createUser(request: AdminUserCreateRequest): UserDto = api.decode(
        UserDto.serializer(),
        api.post(ApiEndpoints.Admin.USERS, api.encode(AdminUserCreateRequest.serializer(), request)),
    )

    suspend fun updateUser(id: Long, request: AdminUserUpdateRequest): UserDto = api.decode(
        UserDto.serializer(),
        api.put(ApiEndpoints.Admin.user(id), api.encode(AdminUserUpdateRequest.serializer(), request)),
    )

    suspend fun permissionUsers(page: Int, perPage: Int, search: String): PermissionUserPageDto = api.decode(
        PermissionUserPageDto.serializer(), api.get(ApiEndpoints.Admin.PERMISSION_USERS,
            mapOf("page" to page.toString(), "per_page" to perPage.toString(), "search" to search.takeIf(String::isNotBlank))),
    )

    suspend fun permissions(): RoleCapabilitiesDto = api.decode(
        RoleCapabilitiesDto.serializer(), api.get(ApiEndpoints.Admin.PERMISSIONS),
    )

    suspend fun updateRole(role: String, capabilities: List<String>): RoleCapabilitiesUpdateRequest = api.decode(
        RoleCapabilitiesUpdateRequest.serializer(),
        api.put(ApiEndpoints.Admin.PERMISSIONS,
            api.encode(RoleCapabilitiesUpdateRequest.serializer(), RoleCapabilitiesUpdateRequest(role, capabilities))),
    )

    suspend fun userPermissions(id: Long): UserCapabilitiesDto = api.decode(
        UserCapabilitiesDto.serializer(), api.get(ApiEndpoints.Admin.userPermissions(id)),
    )

    suspend fun updateUserPermissions(id: Long, capabilities: Map<String, Boolean?>): UserCapabilitiesSavedDto = api.decode(
        UserCapabilitiesSavedDto.serializer(),
        api.put(ApiEndpoints.Admin.userPermissions(id),
            api.encode(UserCapabilitiesUpdateRequest.serializer(), UserCapabilitiesUpdateRequest(capabilities))),
    )

    suspend fun settings(): RuntimeSettingsDto = api.decode(
        RuntimeSettingsDto.serializer(), api.get(ApiEndpoints.Admin.SETTINGS),
    )

    suspend fun updateSettings(request: RuntimeSettingsUpdateRequest): RuntimeSettingsSavedDto = api.decode(
        RuntimeSettingsSavedDto.serializer(), api.post(ApiEndpoints.Admin.SETTINGS,
            api.encode(RuntimeSettingsUpdateRequest.serializer(), request)))
}
