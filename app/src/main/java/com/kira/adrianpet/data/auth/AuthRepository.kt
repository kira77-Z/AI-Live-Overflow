package com.kira.adrianpet.data.auth

import kotlinx.coroutines.flow.StateFlow

data class AuthSession(val userId: String, val email: String?)

sealed interface AuthResult {
    data object Success : AuthResult
    data class Failure(val message: String) : AuthResult
}

class InvalidCredentialsException(cause: Throwable? = null) : Exception(cause)

interface AuthTransport {
    val session: StateFlow<AuthSession?>
    suspend fun signIn(email: String, password: String)
    suspend fun signOut()
}

class AuthRepository(private val transport: AuthTransport) {
    val session: StateFlow<AuthSession?> = transport.session

    suspend fun signIn(email: String, password: String): AuthResult {
        if (email.isBlank() || password.isBlank()) return AuthResult.Failure("请输入邮箱和密码")
        return try {
            transport.signIn(email.trim(), password)
            AuthResult.Success
        } catch (_: InvalidCredentialsException) {
            AuthResult.Failure("邮箱或密码不正确")
        } catch (_: Exception) {
            AuthResult.Failure("暂时无法登录，请检查网络后重试")
        }
    }

    suspend fun signOut() {
        transport.signOut()
    }
}
