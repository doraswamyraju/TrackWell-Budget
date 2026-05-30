package com.example.trackwell.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.widget.Toast
import com.google.android.gms.auth.api.signin.GoogleSignIn
import com.google.android.gms.auth.api.signin.GoogleSignInAccount
import com.google.android.gms.auth.api.signin.GoogleSignInClient
import com.google.android.gms.auth.api.signin.GoogleSignInOptions
import com.google.android.gms.common.api.Scope
import com.google.api.client.googleapis.extensions.android.gms.auth.GoogleAccountCredential
import com.google.api.client.http.FileContent
import com.google.api.client.http.javanet.NetHttpTransport
import com.google.api.client.json.gson.GsonFactory
import com.google.api.services.drive.Drive
import com.google.api.services.drive.DriveScopes
import java.io.File
import java.io.FileOutputStream
import java.util.Collections
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

class BackupManager(private val context: Context) {

    companion object {
        private const val BACKUP_FILE_NAME = "trackwell_backup.db"
    }

    private val gso = GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
        .requestEmail()
        .requestScopes(Scope(DriveScopes.DRIVE_APPDATA), Scope(DriveScopes.DRIVE_FILE))
        .build()

    val googleSignInClient: GoogleSignInClient = GoogleSignIn.getClient(context, gso)

    fun getSignedInAccount(): GoogleSignInAccount? {
        return GoogleSignIn.getLastSignedInAccount(context)
    }

    private fun getDriveService(account: GoogleSignInAccount): Drive {
        val credential = GoogleAccountCredential.usingOAuth2(
            context,
            Collections.singleton(DriveScopes.DRIVE_APPDATA)
        )
        credential.selectedAccount = account.account

        return Drive.Builder(
            NetHttpTransport(),
            GsonFactory.getDefaultInstance(),
            credential
        )
            .setApplicationName("TrackWell")
            .build()
    }

    suspend fun backupToGoogleDrive(onResult: (Boolean, String) -> Unit) = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        if (account == null) {
            showToastOnMain("Please sign in to Google first.")
            onResult(false, "Not signed in")
            return@withContext
        }

        try {
            val driveService = getDriveService(account)
            val dbFile = context.getDatabasePath("trackwell_database")
            if (!dbFile.exists()) {
                onResult(false, "Local database file not found")
                return@withContext
            }

            // Find existing backup file in Google Drive AppData space to overwrite it
            val filesList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .execute()

            val existingFile = filesList.files.firstOrNull()

            val metadata = com.google.api.services.drive.model.File().apply {
                name = BACKUP_FILE_NAME
                parents = Collections.singletonList("appDataFolder")
            }

            val mediaContent = FileContent("application/octet-stream", dbFile)

            if (existingFile != null) {
                // Update existing backup
                driveService.files().update(existingFile.id, null, mediaContent).execute()
            } else {
                // Create new backup
                driveService.files().create(metadata, mediaContent).execute()
            }

            showToastOnMain("Database backup completed successfully! ☁️")
            onResult(true, "Success")
        } catch (e: Exception) {
            e.printStackTrace()
            showToastOnMain("Backup failed: ${e.localizedMessage}")
            onResult(false, e.localizedMessage ?: "Unknown error")
        }
    }

    suspend fun restoreFromGoogleDrive(onResult: (Boolean, String) -> Unit) = withContext(Dispatchers.IO) {
        val account = getSignedInAccount()
        if (account == null) {
            showToastOnMain("Please sign in to Google first.")
            onResult(false, "Not signed in")
            return@withContext
        }

        try {
            val driveService = getDriveService(account)

            // Find database file in Google Drive AppData
            val filesList = driveService.files().list()
                .setSpaces("appDataFolder")
                .setQ("name = '$BACKUP_FILE_NAME'")
                .execute()

            val remoteFile = filesList.files.firstOrNull()
            if (remoteFile == null) {
                showToastOnMain("No cloud backup found on Google Drive.")
                onResult(false, "Backup not found")
                return@withContext
            }

            // Download file to a temp location
            val tempFile = File(context.cacheDir, "temp_restore.db")
            FileOutputStream(tempFile).use { outputStream ->
                driveService.files().get(remoteFile.id)
                    .executeMediaAndDownloadTo(outputStream)
            }

            // Overwrite database
            val dbFile = context.getDatabasePath("trackwell_database")
            
            // Close database before replacing files to avoid corruption
            AppDatabase.getDatabase(context).close()

            if (tempFile.exists()) {
                tempFile.copyTo(dbFile, overwrite = true)
                
                // Copy -shm and -wal journal files if present
                val tempShm = File(context.cacheDir, "temp_restore.db-shm")
                val dbShm = context.getDatabasePath("trackwell_database-shm")
                if (tempShm.exists()) tempShm.copyTo(dbShm, overwrite = true)

                val tempWal = File(context.cacheDir, "temp_restore.db-wal")
                val dbWal = context.getDatabasePath("trackwell_database-wal")
                if (tempWal.exists()) tempWal.copyTo(dbWal, overwrite = true)

                tempFile.delete()
                showToastOnMain("Database restored successfully! App will now update. 🔄")
                onResult(true, "Success")
            } else {
                onResult(false, "Temp file copy error")
            }

        } catch (e: Exception) {
            e.printStackTrace()
            showToastOnMain("Restore failed: ${e.localizedMessage}")
            onResult(false, e.localizedMessage ?: "Unknown error")
        }
    }

    private fun showToastOnMain(message: String) {
        Handler(Looper.getMainLooper()).post {
            Toast.makeText(context, message, Toast.LENGTH_LONG).show()
        }
    }
}
