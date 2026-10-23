package com.mukhtabir.android.testing

import android.content.Context
import androidx.security.crypto.EncryptedFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import org.robolectric.annotation.Implementation
import org.robolectric.annotation.Implements
import org.robolectric.annotation.RealObject
import org.robolectric.util.ReflectionHelpers

/** JVM-only file seam. Tests exercise production storage/index/checksum logic, not AndroidKeyStore encryption. */
@Implements(EncryptedFile.Builder::class)
class OfflineFileBuilderShadow {
    @RealObject private lateinit var builder: EncryptedFile.Builder
    @Implementation fun build(): EncryptedFile {
        val file = ReflectionHelpers.getField<File>(builder, "mFile")
        val context = ReflectionHelpers.getField<Context>(builder, "mContext")
        val constructor = EncryptedFile::class.java.getDeclaredConstructor(File::class.java, String::class.java,
            Class.forName("com.google.crypto.tink.StreamingAead"), Context::class.java)
        constructor.isAccessible = true
        return constructor.newInstance(file, "test-only-key", null, context)
    }
}

@Implements(EncryptedFile::class)
class OfflineFileShadow {
    @RealObject private lateinit var file: EncryptedFile
    @Implementation fun openFileInput(): FileInputStream = FileInputStream(ReflectionHelpers.getField<File>(file, "mFile"))
    @Implementation fun openFileOutput(): FileOutputStream {
        if (rejectWrites) throw IOException("Simulated test storage write failure")
        return FileOutputStream(ReflectionHelpers.getField<File>(file, "mFile"))
    }
    companion object { @Volatile var rejectWrites = false }
}
