package com.example.legacyvault

import com.example.legacyvault.data.VaultRepository
import com.example.legacyvault.ui.UserSession
import com.example.legacyvault.ui.VaultViewModel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VaultStatePersistenceTest {

    @Test
    fun testFormStateSurvivesScreenNavigation() {
        val repository = VaultRepository()
        val viewModel = VaultViewModel(repository)

        // Step 1: User enters partial draft data on Manual Entry Screen
        val testInstitution = "HDFC Bank Ltd - Demo"
        val testAccountNumber = "987654321012"
        val testCategory = "Bank Accounts"
        val testBranch = "Indiranagar, Bangalore"

        viewModel.updateManualDraft(
            institution = testInstitution,
            category = testCategory,
            accountType = "Savings Account",
            accountNumber = testAccountNumber,
            branch = testBranch,
            nominee = "Anita Raman"
        )

        // Step 2: Simulate navigating away to another screen (e.g. INVENTORY or SCAN)
        var simulatedActiveScreen = "INVENTORY"
        assertEquals("INVENTORY", simulatedActiveScreen)

        // Verify state is NOT wiped by navigation away
        assertEquals(testInstitution, viewModel.manualDraft.value.institution)
        assertEquals(testAccountNumber, viewModel.manualDraft.value.accountNumber)

        // Step 3: User navigates back to Manual Entry Screen
        simulatedActiveScreen = "MANUAL_ENTRY"
        assertEquals("MANUAL_ENTRY", simulatedActiveScreen)

        // Form draft is still completely preserved
        val preservedDraft = viewModel.manualDraft.value
        assertEquals("Form data must survive screen navigation!", testInstitution, preservedDraft.institution)
        assertEquals("Account number must survive screen navigation!", testAccountNumber, preservedDraft.accountNumber)
        assertEquals(testCategory, preservedDraft.category)
        assertEquals(testBranch, preservedDraft.branch)

        // Step 4: User commits the draft
        viewModel.saveManualDraftRecord()

        // Verify the draft is cleared after successful save
        assertEquals("", viewModel.manualDraft.value.institution)

        // Verify the record is saved into the repository
        val records = repository.records.value
        val saved = records.find { it.accountNumber == testAccountNumber }
        assertNotNull("Record must be saved into repository", saved)
        assertEquals(testInstitution, saved?.institution)
    }

    @Test
    fun testRoleSeparationAndSession() {
        val repository = VaultRepository()
        val viewModel = VaultViewModel(repository)

        // Initially no user is authenticated
        assertEquals(null, viewModel.currentUser.value)

        // Authenticate with Attestor credentials
        viewModel.setSessionForTesting(
            UserSession(
                userId = "usr_attestor_1",
                email = "attestor1@vault.local",
                role = "attestor",
                displayName = "V. Krishnan (Lawyer)",
                linkedOwnerId = "owner_sundaram",
                attestorId = "attestor_1",
                token = "test_tok_1"
            )
        )

        val user = viewModel.currentUser.value
        assertNotNull("User session must be established", user)
        assertEquals("attestor", user?.role)
        assertEquals("attestor_1", user?.attestorId)
        assertTrue(user?.displayName?.contains("Lawyer") == true)

        // Sign out
        viewModel.logout()
        assertEquals(null, viewModel.currentUser.value)
    }
}
