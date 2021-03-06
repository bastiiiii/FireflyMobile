package xyz.hisname.fireflyiii.repository.bills

import android.app.Application
import androidx.lifecycle.LiveData
import androidx.lifecycle.MediatorLiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import xyz.hisname.fireflyiii.data.local.dao.AppDatabase
import xyz.hisname.fireflyiii.data.remote.firefly.api.BillsService
import xyz.hisname.fireflyiii.repository.BaseViewModel
import xyz.hisname.fireflyiii.repository.models.ApiResponses
import xyz.hisname.fireflyiii.repository.models.bills.BillData
import xyz.hisname.fireflyiii.repository.models.bills.BillSuccessModel
import xyz.hisname.fireflyiii.repository.models.error.ErrorModel
import xyz.hisname.fireflyiii.util.network.HttpConstants
import xyz.hisname.fireflyiii.util.network.retrofitCallback
import xyz.hisname.fireflyiii.workers.bill.DeleteBillWorker

class BillsViewModel(application: Application): BaseViewModel(application) {

    private val repository: BillRepository
    private var billData: MutableList<BillData> = arrayListOf()
    private val billsService by lazy { genericService()?.create(BillsService::class.java) }

    init {
        val billDataDao = AppDatabase.getInstance(application).billDataDao()
        repository = BillRepository(billDataDao, billsService)
    }


    fun getPaginatedBills(pageNumber: Int, startDate: String, endDate: String): LiveData<MutableList<BillData>>{
        val data: MutableLiveData<MutableList<BillData>> = MutableLiveData()
        viewModelScope.launch(Dispatchers.IO){
            repository.getPaginatedBills(pageNumber, startDate, endDate).collectLatest {
                data.postValue(it)
            }
        }
        return data
    }

    fun getBillById(billId: Long): LiveData<MutableList<BillData>>{
        val billLiveData: MutableLiveData<MutableList<BillData>> = MutableLiveData()
        viewModelScope.launch(Dispatchers.IO){
            billData = repository.retrieveBillById(billId)
        }.invokeOnCompletion {
            billLiveData.postValue(billData)
        }
        return billLiveData
    }

    fun deleteBillById(billId: Long): LiveData<Boolean>{
        val isDeleted: MutableLiveData<Boolean> = MutableLiveData()
        var isItDeleted = 0
        isLoading.value = true
        viewModelScope.launch(Dispatchers.IO){
            isItDeleted = repository.deleteBillById(billId)
        }.invokeOnCompletion {
            when (isItDeleted) {
                HttpConstants.FAILED -> {
                    isDeleted.postValue(false)
                    DeleteBillWorker.initPeriodicWorker(billId, getApplication())
                }
                HttpConstants.UNAUTHORISED -> {
                    isDeleted.postValue(false)
                }
                HttpConstants.NO_CONTENT_SUCCESS -> {
                    isDeleted.postValue(true)
                }
            }
            isLoading.postValue(false)
        }
        return isDeleted

    }

    fun addBill(name: String, amountMin: String, amountMax: String, date: String, repeatFreq: String,
                skip: String, active: String,currencyId: String,notes: String?): LiveData<ApiResponses<BillSuccessModel>>{
        val apiResponse = MediatorLiveData<ApiResponses<BillSuccessModel>>()
        val apiLiveData: MutableLiveData<ApiResponses<BillSuccessModel>> = MutableLiveData()
        billsService?.createBill(name, amountMin, amountMax, date,
                repeatFreq, skip, active, currencyId, notes)?.enqueue(retrofitCallback(
                { response ->
                    var errorMessage = ""
                    val responseErrorBody = response.errorBody()
                    if (responseErrorBody != null) {
                        errorMessage = String(responseErrorBody.bytes())
                        val gson = Gson().fromJson(errorMessage, ErrorModel::class.java)
                        errorMessage = when {
                            gson.errors.name != null -> gson.errors.name[0]
                            gson.errors.currency_code != null -> gson.errors.currency_code[0]
                            gson.errors.amount_min != null -> gson.errors.amount_min[0]
                            gson.errors.repeat_freq != null -> gson.errors.repeat_freq[0]
                            gson.errors.date != null -> gson.errors.date[0]
                            gson.errors.skip != null -> gson.errors.skip[0]
                            else -> "Error occurred while saving bill"
                        }
                    }
                    val networkData = response.body()
                    if (networkData != null) {
                        viewModelScope.launch(Dispatchers.IO) { repository.insertBill(networkData.data) }
                        apiLiveData.postValue(ApiResponses(response.body()))
                    } else {
                        apiLiveData.postValue(ApiResponses(errorMessage))
                    }
                })
        { throwable -> apiLiveData.postValue(ApiResponses(throwable)) })
        apiResponse.addSource(apiLiveData){ apiResponse.value = it }
        return apiResponse
    }

    fun updateBill(billId: Long, name: String, amountMin: String, amountMax: String, date: String,
                   repeatFreq: String, skip: String,active: String,currencyId:
                   String,notes: String?): LiveData<ApiResponses<BillSuccessModel>>{
        val apiResponse = MediatorLiveData<ApiResponses<BillSuccessModel>>()
        val apiLiveData: MutableLiveData<ApiResponses<BillSuccessModel>> = MutableLiveData()
        billsService?.updateBill(billId, name, amountMin, amountMax, date,
                repeatFreq, skip, active, currencyId, notes)?.enqueue(retrofitCallback(
                { response ->
                    var errorBody = ""
                    if (response.errorBody() != null) {
                        errorBody = String(response.errorBody()?.bytes()!!)
                    }
                    val networkData = response.body()
                    if (networkData != null) {
                        viewModelScope.launch(Dispatchers.IO) { repository.insertBill(networkData.data) }
                        apiLiveData.postValue(ApiResponses(response.body()))
                    } else {
                        apiLiveData.postValue(ApiResponses(errorBody))
                    }
                })
        { throwable -> apiLiveData.postValue(ApiResponses(throwable)) })
        apiResponse.addSource(apiLiveData){ apiResponse.value = it }
        return apiResponse
    }

    fun deleteBillByName(billName: String): LiveData<Boolean>{
        val isDeleted: MutableLiveData<Boolean> = MutableLiveData()
        var isItDeleted = 0
        var billId: Long = 0
        viewModelScope.launch(Dispatchers.IO) {
            billId = repository.getBillByName(billName)[0].billId ?: 0
            if (billId != 0L) {
                isItDeleted = repository.deleteBillById(billId)
            }
        }.invokeOnCompletion {
            // Since onDraw() is being called multiple times, we check if the bill exists locally in the DB.
            when (isItDeleted) {
                HttpConstants.FAILED -> {
                    isDeleted.postValue(false)
                    DeleteBillWorker.initPeriodicWorker(billId, getApplication())
                }
                HttpConstants.UNAUTHORISED -> {
                    isDeleted.postValue(false)
                }
                HttpConstants.NO_CONTENT_SUCCESS -> {
                    isDeleted.postValue(true)
                }
            }
        }
        return isDeleted
    }

}