package com.kira.adrianpet.data.auth

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthRepositoryTest {
    @Test
    fun `successful login is reflected by persisted transport session`() = runBlocking {
        val transport = FakeAuthTransport()
        val repository = AuthRepository(transport)

        assertEquals(AuthResult.Success, repository.signIn("cat@example.com", "secret-password"))
        assertEquals("user-1", repository.session.value?.userId)
    }

    @Test
    fun `invalid credentials surface a readable error`() = runBlocking {
        val repository = AuthRepository(FakeAuthTransport(failLogin = true))

        val result = repository.signIn("cat@example.com", "wrong")

        assertTrue(result is AuthResult.Failure)
        assertEquals("邮箱或密码不正确", (result as AuthResult.Failure).message)
    }

    @Test
    fun `sign out clears session`() = runBlocking {
        val transport = FakeAuthTransport()
        val repository = AuthRepository(transport)
        repository.signIn("cat@example.com", "secret-password")

        repository.signOut()

        assertNull(repository.session.value)
    }

    @Test
    fun `repository never retains the password`() = runBlocking {
        val transport = FakeAuthTransport()
        val repository = AuthRepository(transport)
        repository.signIn("cat@example.com", "secret-password")

        assertEquals("secret-password", transport.passwordReceivedOnce)
        assertTrue(repository.toString().contains("secret-password").not())
    }
}

private class FakeAuthTransport(private val failLogin: Boolean = false) : AuthTransport {
    override val session = MutableStateFlow<AuthSession?>(null)
    var passwordReceivedOnce: String? = null

    override suspend fun signIn(email: String, password: String) {
        passwordReceivedOnce = password
        if (failLogin) throw InvalidCredentialsException()
        session.value = AuthSession("user-1", email)
    }

    override suspend fun signOut() {
        session.value = null
    }
}
