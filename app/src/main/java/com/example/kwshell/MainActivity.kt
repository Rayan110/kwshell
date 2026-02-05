package com.example.kwshell

import android.content.ComponentName
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.ScrollView
import androidx.appcompat.app.AppCompatActivity
import com.example.kwshell.databinding.ActivityMainBinding
import rikka.shizuku.Shizuku
import rikka.shizuku.Shizuku.OnRequestPermissionResultListener
import rikka.shizuku.Shizuku.UserServiceArgs

class MainActivity : AppCompatActivity(), OnRequestPermissionResultListener, ServiceConnection {

    private lateinit var binding: ActivityMainBinding
    private var userService: IUserService? = null

    private val binderReceivedListener = Shizuku.OnBinderReceivedListener {
        updateUiState()
        if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED) {
            bindUserService()
        }
    }

    private val binderDeadListener = Shizuku.OnBinderDeadListener {
        updateUiState()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        Shizuku.addRequestPermissionResultListener(this)
        Shizuku.addBinderReceivedListener(binderReceivedListener)
        Shizuku.addBinderDeadListener(binderDeadListener)

        binding.executeButton.setOnClickListener {
            if (Shizuku.isPreV11() || Shizuku.checkSelfPermission() != PackageManager.PERMISSION_GRANTED) {
                Shizuku.requestPermission(SHIZUKU_REQUEST_CODE)
            } else {
                executeCommand(binding.commandInput.text.toString())
            }
        }

        binding.checkAgainButton.setOnClickListener {
            updateUiState()
        }

        updateUiState()
    }

    override fun onDestroy() {
        super.onDestroy()
        Shizuku.removeRequestPermissionResultListener(this)
        Shizuku.removeBinderReceivedListener(binderReceivedListener)
        Shizuku.removeBinderDeadListener(binderDeadListener)
        unbindUserService()
    }

    override fun onRequestPermissionResult(requestCode: Int, grantResult: Int) {
        if (requestCode == SHIZUKU_REQUEST_CODE) {
            updateUiState()
            if (grantResult == PackageManager.PERMISSION_GRANTED) {
                bindUserService()
            }
        }
    }

    override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
        userService = IUserService.Stub.asInterface(service)
        updateUiState()
    }

    override fun onServiceDisconnected(name: ComponentName?) {
        userService = null
        updateUiState()
    }

    private fun bindUserService() {
        val componentName = ComponentName(packageName, UserService::class.java.name)
        val userServiceArgs = UserServiceArgs(componentName)
            .daemon(false)
            .processNameSuffix(":service")
            .debuggable(true)
            .version(1)

        Shizuku.bindUserService(userServiceArgs, this)
    }

    private fun unbindUserService() {
        if (userService != null) {
            Shizuku.unbindUserService(
                UserServiceArgs(ComponentName(packageName, UserService::class.java.name)),
                this,
                true
            )
        }
    }

    private fun updateUiState() {
        if (!Shizuku.pingBinder()) {
            binding.gatekeeperView.visibility = View.VISIBLE
            binding.commandInput.isEnabled = false
            binding.executeButton.isEnabled = false
            return
        }

        binding.gatekeeperView.visibility = View.GONE

        val isGranted = Shizuku.checkSelfPermission() == PackageManager.PERMISSION_GRANTED
        val isUserServiceBound = userService != null

        binding.commandInput.isEnabled = isGranted && isUserServiceBound
        binding.executeButton.isEnabled = isGranted && isUserServiceBound

        if (!isGranted) {
            binding.terminalOutput.append("\n> Shizuku permission not granted. Please grant permission to use the terminal.\n")
        } else if (!isUserServiceBound) {
            binding.terminalOutput.append("\n> Connecting to user service...\n")
        } else {
            binding.terminalOutput.append("\n> Shizuku is ready!\n")
        }
    }

    private fun executeCommand(command: String) {
        if (command.isBlank()) {
            return
        }

        val output = userService?.executeCommand(command) ?: "Error: UserService not connected."
        binding.terminalOutput.append("> $command\n$output\n")
        binding.commandInput.text.clear()
        binding.scrollView.post { binding.scrollView.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    companion object {
        private const val SHIZUKU_REQUEST_CODE = 123
    }
}