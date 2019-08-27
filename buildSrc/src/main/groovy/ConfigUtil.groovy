import org.apache.commons.io.FileUtils
import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle
import org.gradle.api.tasks.TaskState

import java.text.SimpleDateFormat

class ConfigUtil {

    /**
     * 获取依赖的插件
     */
    static getApplyPlugins() {
        def plugins = getDepConfigByFilter(new DepConfigFilter() {
            @Override
            boolean accept(String name, DepConfig config) {
                if (!name.startsWith("plugin.")) return false
                if (!config.isApply) return false
                return true
            }
        })
        GLog.d("getApplyPlugins = ${GLog.object2String(plugins)}")
        return plugins
    }

    /**
     * 获取依赖的 pkg
     */
    static getApplyPkgs(boolean isApplyAll, Project project) {
        def applyPkgs = getDepConfigFilter(new DepConfigFilter() {
            @Override
            boolean accept(String name, DepConfig config) {
                if (!config.isApply) return false
                boolean isAccept = name.endsWith(".pkg")
                //launcher 是所有 pkg 全量导入，core 下的 app 只导入自己的 pkg, :chat:app -> :chat:pkg
                if (isAccept && !isApplyAll) {
                    isAccept &= project.path.contains(name.split("\\.")[1])
                }
                return isAccept
            }
        })
        GLog.d("getApplyPkgs = ${GLog.object2String(applyPkgs)}")
        return applyPkgs
    }

    /**
     * 获取依赖的 export
     */
    static getApplyExports() {
        def applyExports = getDepConfigFilter(new DepConfigFilter() {
            @Override
            boolean accept(String name, DepConfig config) {
                if (!config.isApply) return false
                return name.endsWith(".export")
            }
        })
        GLog.d("getApplyExports = ${GLog.object2String(applyExports)}")
        return applyExports
    }

    static addBuildListener(Gradle g) {
        g.addBuildListener(new ConfigBuildListener())
    }

    private static class ConfigBuildListener implements BuildListener {

        private List<TaskInfo> taskInfoList = []
        private long startBuildMillis

        @Override
        void buildStarted(Gradle gradle) {
            GLog.d("buildStarted")
        }

        @Override
        void settingsEvaluated(Settings settings) {
            startBuildMillis = System.currentTimeMillis()
            GLog.d("settingsEvaluated")
            includeModule(settings)
        }

        @Override
        void projectsLoaded(Gradle gradle) {
            GLog.d("projectsLoaded")
            generateDep(gradle)
            gradle.addProjectEvaluationListener(new ProjectEvaluationListener() {
                /**
                 * 执行在各 module 的 build.gradle 之前
                 */
                @Override
                void beforeEvaluate(Project project) {
                    GLog.d("beforeEvaluate")
                    // 定位到具体 project
                    if (project.subprojects.isEmpty()) {
                        if (project.name == "app" || project.name == "launcher") {
                            GLog.d("${project.toString()} applies app_config.gradle")
                            project.apply {
                                from "${project.rootDir.path}/app_config.gradle"
                            }
                        } else {
                            GLog.d("${project.toString()} applies lib_config.gradle")
                            project.apply {
                                from "${project.rootDir.path}/lib_config.gradle"
                            }
                        }
                    }
                }

                /**
                 * 执行在各 module 的 build.gradle 之后
                 */
                @Override
                void afterEvaluate(Project project, ProjectState projectState) {
                    GLog.d("afterEvaluate")
                }
            })
        }

        @Override
        void projectsEvaluated(Gradle gradle) {
            GLog.d("projectsEvaluated")
            gradle.addListener(new TaskExecutionListener() {

                long startTime

                @Override
                void beforeExecute(Task task) {
                    startTime = System.currentTimeMillis()
                }

                @Override
                void afterExecute(Task task, TaskState taskState) {
                    def exeDuration = System.currentTimeMillis() - startTime
                    if (exeDuration > 100) {
                        taskInfoList.add(new TaskInfo(task, exeDuration))
                    }
                }
            })
        }

        @Override
        void buildFinished(BuildResult buildResult) {
            GLog.d("buildFinished")
            if (!taskInfoList.isEmpty()) {
                Collections.sort(taskInfoList, new Comparator<TaskInfo>() {
                    @Override
                    int compare(TaskInfo t, TaskInfo t1) {
                        return t1.executeDuration - t.executeDuration
                    }
                })
                StringBuilder sb = new StringBuilder()
                int buildSec = (System.currentTimeMillis() - startBuildMillis) / 1000
                int m = buildSec / 60
                int s = buildSec % 60
                def timeInfo = (m == 0 ? "${s}s" : "${m}m ${s}s (${buildSec}s)")
                sb.append("BUILD FINISHED in $timeInfo\n")
                taskInfoList.each {
                    sb.append(String.format("%7sms %s\n", it.executeDuration, it.task.path))
                }
                def content = sb.toString()
                GLog.l(content)
                File file = new File(buildResult.gradle.rootProject.buildDir.getAbsolutePath(),
                        "build_time_records_" + new SimpleDateFormat("yyyy-MM-dd-HH-mm-ss").format(new Date()) + ".txt")
                FileUtils.write(file, content)
            }
        }

        private static includeModule(Settings settings) {
            def config = getDepConfigFilter(new DepConfigFilter(){
                @Override
                boolean accept(String name, DepConfig config) {
                    if (name.endsWith(".app")) {
                        def appName = name.substring('core.'.length(), name.length() - '.app'.length())
                        if (!Config.appConfig.contains(appName)) {
                            config.isApply = false
                        }
                    }
                    // 如果 Config.pkgConfig 不为空，说明是 pkg 调试模式
                    if (!Config.pkgConfig.isEmpty()) {
                        if (name.endsWith(".pkg")) {
                            def pkgName = name.substring('core.'.length(), name.length() - '.app'.length())
                            if (!Config.pkgConfig.contains(pkgName)) {
                                config.isApply = false
                            }
                        }
                    }
                    // 过滤出本地并且 apply 的模块
                    if (!config.isApply) return false
                    if (!config.isUseLocal) return false
                    if (config.localPath == "") return false
                    return true
                }
            }).each { _, cfg ->
                settings.include(cfg.localPath)
            }
            GLog.l("includeModule = ${GLog.object2String(config)}")
        }

        /**
         * 根据 DepConfig 生成 dep
         */
        private static generateDep(Gradle gradle) {
            def config = getDepConfigFilter(new DepConfigFilter(){
                @Override
                boolean accept(String name, DepConfig config) {
                    if (config.isUseLocal) {    //使用本地依赖
                        config.dep = gradle.rootProject.findProject(config.localPath)
                    } else {    //使用远程依赖
                        config.dep = config.remotePath
                    }
                    return true
                }
            })
            GLog.l("generateDep = ${GLog.object2String(config)}")
        }

        private static class TaskInfo {
            Task task
            long executeDuration

            TaskInfo(Task task, long executeDuration) {
                this.task = task
                this.executeDuration = executeDuration
            }
        }
    }

    /**
     * 根据过滤器获取 DepConfig
     */
    static Map<String, DepConfig> getDepConfigFilter(DepConfigFilter filter) {
        _getDepConfigFilter("", Config.depConfig, filter)
    }

    private static _getDepConfigFilter(String namePrefix, Map map, DepConfigFilter filter) {
        def depConfigList = [:]
        for (Map.Entry entry : map.entrySet()) {
            def (name, value) = [entry.key, entry.value]
            if (value instanceof Map) {
                namePrefix += (name + '.')
                depConfigList.putAll(_getDepConfigFilter(namePrefix, value, filter))
                namePrefix -= (name + '.')
                continue
            }
            def config = value as DepConfig
            if (filter == null || filter.accept(namePrefix + name, config)) {
                depConfigList.put(namePrefix + name, config)    //添加符合条件的结果
            }
        }
        return depConfigList
    }

    interface DepConfigFilter {
        boolean accept(String name, DepConfig config)
    }
}