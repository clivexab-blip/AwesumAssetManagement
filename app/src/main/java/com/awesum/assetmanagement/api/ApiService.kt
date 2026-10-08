package com.awesum.assetmanagement.api

import com.awesum.assetmanagement.model.*
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.*

interface ApiService {

    @GET("get_orders.php")
    suspend fun getOrders(): Response<ApiResponse<List<Order>>>

    @FormUrlEncoded
    @POST("get_or_create_job_for_order.php")
    suspend fun getOrCreateJobForOrder(
        @Field("order_id") orderId: Int,
        @Field("user_id") userId: Int
    ): Response<ApiResponse<Job>>

    @GET("get_job.php")
    suspend fun getJob(@Query("job_id") jobId: Int): Response<ApiResponse<Job>>

    @GET("get_assets.php")
    suspend fun getAssets(@Query("job_id") jobId: Int): Response<ApiResponse<List<Asset>>>

    @FormUrlEncoded
    @POST("add_asset.php")
    suspend fun addAsset(
        @Field("job_id") jobId: Int,
        @Field("name") name: String,
        @Field("category") category: String,
        @Field("status") status: String,
        @Field("quantity") quantity: Int,
        @Field("notes") notes: String
    ): Response<ApiResponse<Map<String, Int>>>

    @GET("get_team.php")
    suspend fun getTeam(@Query("job_id") jobId: Int): Response<ApiResponse<List<TeamMember>>>

    @FormUrlEncoded
    @POST("add_member.php")
    suspend fun addMember(
        @Field("job_id") jobId: Int,
        @Field("full_name") fullName: String,
        @Field("role") role: String,
        @Field("phone") phone: String
    ): Response<ApiResponse<Map<String, Int>>>

    @GET("get_photos.php")
    suspend fun getPhotos(@Query("job_id") jobId: Int): Response<ApiResponse<List<SitePhoto>>>

    @Multipart
    @POST("upload_photo.php")
    suspend fun uploadPhoto(
        @Part("job_id") jobId: RequestBody,
        @Part("caption") caption: RequestBody,
        @Part photo: MultipartBody.Part
    ): Response<ApiResponse<Map<String, String>>>

    @FormUrlEncoded
    @POST("save_reflection.php")
    suspend fun saveReflection(
        @Field("job_id") jobId: Int,
        @Field("notes") notes: String,
        @Field("team_must_return") teamMustReturn: Int,
        @Field("submitted") submitted: Int
    ): Response<ApiResponse<Any>>

    @FormUrlEncoded
    @POST("submit_job_log.php")
    suspend fun submitJobLog(
        @Field("job_id") jobId: Int,
        @Field("draft") draft: Int
    ): Response<ApiResponse<Any>>

    @FormUrlEncoded
    @POST("sync_user.php")
    suspend fun syncUser(
        @Field("firebase_uid") firebaseUid: String,
        @Field("name") name: String,
        @Field("email") email: String,
        @Field("auth_provider") authProvider: String,
        @Field("role") role: String
    ): Response<ApiResponse<SyncUserResponse>>
}
