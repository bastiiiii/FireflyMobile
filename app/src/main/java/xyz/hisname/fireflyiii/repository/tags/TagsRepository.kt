package xyz.hisname.fireflyiii.repository.tags

import androidx.annotation.WorkerThread
import xyz.hisname.fireflyiii.data.local.dao.TagsDataDao
import xyz.hisname.fireflyiii.data.remote.firefly.api.TagsService
import xyz.hisname.fireflyiii.repository.models.tags.TagsData

class TagsRepository(private val tagsDataDao: TagsDataDao,
                     private val tagsService: TagsService?) {

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun insertTags(tags: TagsData){
        tagsDataDao.insert(tags)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun deleteTagByName(tagName: String): Int{
        return tagsDataDao.deleteTagByName(tagName)
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun allTags(): MutableList<TagsData>{
        try {
            val tagsData: MutableList<TagsData> = arrayListOf()
            val networkCall = tagsService?.getPaginatedTags(1)
            val responseBody = networkCall?.body()
            if (responseBody != null && networkCall.isSuccessful) {
                tagsData.addAll(responseBody.data.toMutableList())
                val pagination = responseBody.meta.pagination
                if (pagination.total_pages != pagination.current_page) {
                    for (items in 2..pagination.total_pages) {
                        tagsData.addAll(
                                tagsService?.getPaginatedTags(items)?.body()?.data?.toMutableList()
                                        ?: arrayListOf()
                        )

                    }
                }
                tagsDataDao.deleteTags()
                tagsData.forEachIndexed { _, data ->
                    insertTags(data)
                }
            }
        } catch (exception: Exception){ }
        return tagsDataDao.getAllTags()
    }

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun retrieveTagById(tagId: Long) = tagsDataDao.getTagById(tagId)

    @Suppress("RedundantSuspendModifier")
    @WorkerThread
    suspend fun retrieveTagByName(tagName: String) = tagsDataDao.getTagByName(tagName)

}