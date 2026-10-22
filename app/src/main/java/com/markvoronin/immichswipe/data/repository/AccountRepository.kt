package com.markvoronin.immichswipe.data.repository

import com.markvoronin.immichswipe.core.SecretStore
import com.markvoronin.immichswipe.data.local.dao.UserAccountDao
import com.markvoronin.immichswipe.data.local.entity.UserAccountEntity
import com.markvoronin.immichswipe.domain.model.User
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccountRepository @Inject constructor(private val userAccountDao: UserAccountDao) : AccountSaver {

    private val secrets = SecretStore.default

    // Callers always see the decrypted key; only the database row holds the encrypted form.
    val allAccounts: Flow<List<UserAccountEntity>> =
        userAccountDao.getAllAccounts().map { list -> list.map { it.decrypted() } }

    private fun UserAccountEntity.decrypted() = copy(apiKey = secrets.decrypt(apiKey) ?: "")

    override suspend fun saveAccount(baseUrl: String, apiKey: String, user: User) {
        val entity = UserAccountEntity(
            userId = user.id,
            baseUrl = baseUrl,
            apiKey = secrets.encrypt(apiKey),
            userName = user.name,
            userEmail = user.email,
            avatarColor = user.avatarColor,
            lastActive = System.currentTimeMillis()
        )
        userAccountDao.insertAccount(entity)
    }

    suspend fun getAccount(userId: String): UserAccountEntity? {
        return userAccountDao.getAccountById(userId)?.decrypted()
    }

    suspend fun deleteAccount(userId: String) {
        val account = userAccountDao.getAccountById(userId)
        if (account != null) {
            userAccountDao.deleteAccount(account)
        }
    }

    /** Encrypts API keys that were stored as plaintext by older versions. */
    suspend fun encryptLegacyApiKeys() {
        userAccountDao.getAllAccountsOnce()
            .filter { secrets.needsEncryption(it.apiKey) }
            .forEach { userAccountDao.insertAccount(it.copy(apiKey = secrets.encrypt(it.apiKey))) }
    }

    suspend fun deleteAllAccounts() {
        userAccountDao.deleteAllAccounts()
    }

    suspend fun updateLastActive(userId: String) {
        userAccountDao.updateLastActive(userId)
    }
}