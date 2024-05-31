package com.hxb.restart

import com.intellij.execution.ExecutionException
import com.intellij.execution.ExecutionManager
import com.intellij.execution.ExecutorRegistry
import com.intellij.execution.RunManager
import com.intellij.execution.runners.ExecutionEnvironment
import com.intellij.execution.runners.ProgramRunner
import com.intellij.execution.ui.RunContentDescriptor
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.components.ProjectComponent
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.wm.ToolWindowId
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.ServerSocket
import java.util.function.Consumer


class ProjectStarterService : ProjectComponent {
    private var serverSocket: ServerSocket? = null
    private var currentProject: Project? = null
    val logger = com.intellij.openapi.diagnostic.Logger.getInstance(ProjectStarterService::class.java)

    override fun initComponent() {
        logger.info("ProjectStarterService Plugin is running.")

        Thread {

            logger.info("ProjectStarterService Plugin is running 1.")
            serverSocket = ServerSocket(43211)
            while (true) {
                try {
                    logger.info("ProjectStarterService Plugin is running 2.")
                    val socket = serverSocket!!.accept()
                    logger.info("ProjectStarterService Plugin is running 3.")
                    val `in` = BufferedReader(InputStreamReader(socket.getInputStream()))
                    val message = `in`.readLine()
                    if ("restart" == message) {
                        // 重启 Spring Boot 项目
                        ApplicationManager.getApplication().invokeLater {
                            restartSpringBootProject()
                        }
                    }

                } catch (e: Exception) {
                    logger.error("ProjectStarterService Plugin is error 4.", e)
                }
            }

        }.start()
    }

    override fun disposeComponent() {
        if (serverSocket != null) {
            try {
                serverSocket!!.close()
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    @Throws(ExecutionException::class)
    private fun restartSpringBootProject() {
        val openProjects = ProjectManager.getInstance().openProjects
        logger.info("ProjectStarterService Plugin is running size.," + openProjects.size)
        if (openProjects.isNotEmpty()) {
            currentProject = openProjects[0]  // 获取第一个打开的项目
            logger.info("ProjectStarterService Plugin is running name.," + currentProject!!.name)
        }
        val runManager = RunManager.getInstance(currentProject!!)
        val configuration = runManager.selectedConfiguration
        if (configuration != null) {
            logger.info("ProjectStarterService Plugin is running cf name.," + configuration!!.name)
            val runConfiguration = configuration.configuration
            val executor = ExecutorRegistry.getInstance().getExecutorById(ToolWindowId.RUN)
            val runner = ProgramRunner.getRunner(ToolWindowId.DEBUG, runConfiguration)
            if (runner != null) {
                // Stop the current running configuration
                // Stop the current running configuration
                ExecutionManager.getInstance(currentProject!!)
                    .getContentManager().allDescriptors.forEach(Consumer<RunContentDescriptor> { descriptor: RunContentDescriptor ->
                        if (descriptor.processHandler!!.isProcessTerminating || descriptor.processHandler!!
                                .isProcessTerminated
                        ) {
                            return@Consumer
                        }
                        descriptor.processHandler!!.destroyProcess()
                    })
                logger.info("ProjectStarterService Plugin is running runner name.," + runner!!.runnerId)
                val environment = executor?.let { ExecutionEnvironment(it, runner, configuration, currentProject!!) }
                if (environment != null) {
                    logger.info("ProjectStarterService Plugin is running execute.,")
                    runner.execute(environment).run { }
                }
            }
        }
    }
}