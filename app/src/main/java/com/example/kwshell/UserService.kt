package com.example.kwshell

import android.content.pm.PackageManager
import rikka.shizuku.Shizuku
import rikka.shizuku.ShizukuBinderWrapper
import java.io.BufferedReader
import java.io.InputStreamReader

class UserService : IUserService.Stub() {

    override fun executeCommand(command: String): String {
        if (Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
            return "Shizuku permission not granted."
        }

        val output = StringBuilder()
        try {
            val process = Runtime.getRuntime().exec(command.split(" ").toTypedArray())
            val reader = BufferedReader(InputStreamReader(process.inputStream))
            val errorReader = BufferedReader(InputStreamReader(process.errorStream))
            var line: String?

            while (reader.readLine().also { line = it } != null) {
                output.append(line).append("\n")
            }
            while (errorReader.readLine().also { line = it } != null) {
                output.append("Error: ").append(line).append("\n")
            }
            process.waitFor()
        } catch (e: Exception) {
            output.append("Error: ").append(e.message)
        }
        return output.toString()
    }

    override fun asBinder() = ShizukuBinderWrapper(this)
}