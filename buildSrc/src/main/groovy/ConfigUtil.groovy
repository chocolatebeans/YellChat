import org.gradle.BuildListener
import org.gradle.BuildResult
import org.gradle.api.Project
import org.gradle.api.ProjectEvaluationListener
import org.gradle.api.ProjectState
import org.gradle.api.initialization.Settings
import org.gradle.api.invocation.Gradle

class ConfigUtil {
    static addBuildListener(Gradle g) {
        g.addBuildListener(new ConfigBuildListener())
    }

    private static class ConfigBuildListener implements BuildListener {
        @Override
        void buildStarted(Gradle gradle) {
            GLog.d("buildStarted")
        }

        @Override
        void settingsEvaluated(Settings settings) {
            GLog.d("settingsEvaluated")
            includeModule(settings)
        }

        @Override
        void projectsLoaded(Gradle gradle) {
            GLog.d("projectsLoaded")
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
        }

        @Override
        void buildFinished(BuildResult buildResult) {
            GLog.d("buildFinished")
        }

        private static includeModule(Settings settings) {
            settings.include(
                    ':app:launcher',
                    ':base:net', ':base:util', ':base:arch', ':base:livedatabus', ':base:widget',
                    ':core:common',
                    ':core:chat:app',':core:chat:pkg',':core:chat:export',
                    ':core:user:app', ':core:user:pkg', ':core:user:export'
            )
        }
    }
}