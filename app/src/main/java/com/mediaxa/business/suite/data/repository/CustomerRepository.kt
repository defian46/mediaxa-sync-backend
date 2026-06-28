package com.mediaxa.business.suite.data.repository

import com.mediaxa.business.suite.data.local.datasource.LocalDataSource
import com.mediaxa.business.suite.data.local.entity.Customer
import kotlinx.coroutines.flow.Flow

class CustomerRepository(private val localDataSource: LocalDataSource) {

    suspend fun insertCustomer(customer: Customer): Long {
        return localDataSource.customerDao.insertCustomer(customer)
    }

    suspend fun updateCustomer(customer: Customer) {
        localDataSource.customerDao.updateCustomer(customer)
    }

    suspend fun getCustomerByUuid(uuid: String): Customer? {
        return localDataSource.customerDao.getCustomerByUuid(uuid)
    }

    suspend fun getCustomerByCode(storeId: Long, code: String): Customer? {
        return localDataSource.customerDao.getCustomerByCode(storeId, code)
    }

    suspend fun getCustomerByPhone(storeId: Long, phone: String): Customer? {
        return localDataSource.customerDao.getCustomerByPhone(storeId, phone)
    }

    fun getAllCustomersFlow(storeId: Long): Flow<List<Customer>> {
        return localDataSource.customerDao.getAllCustomersFlow(storeId)
    }

    fun searchCustomersFlow(storeId: Long, query: String): Flow<List<Customer>> {
        return localDataSource.customerDao.searchCustomersFlow(storeId, query)
    }

    fun getTopCustomersFlow(storeId: Long): Flow<List<Customer>> {
        return localDataSource.customerDao.getTopCustomersFlow(storeId)
    }

    suspend fun getTopCustomers(storeId: Long, limit: Int): List<Customer> {
        return localDataSource.customerDao.getTopCustomers(storeId, limit)
    }

    suspend fun getNewCustomersCount(storeId: Long, startDate: Long): Int {
        return localDataSource.customerDao.getNewCustomersCount(storeId, startDate)
    }

    suspend fun getActiveCustomersCount(storeId: Long, startDate: Long): Int {
        return localDataSource.customerDao.getActiveCustomersCount(storeId, startDate)
    }

    suspend fun getFavoriteMenuUuid(storeId: Long, customerUuid: String): String? {
        return localDataSource.customerDao.getFavoriteMenuUuid(storeId, customerUuid)
    }
}
