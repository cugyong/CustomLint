import org.gradle.api.Plugin
import org.gradle.api.Project

class CustomLintPlugin implements Plugin<Project> {

    def hookFileName = 'post-commit'

    // lint将要扫描的文件默认类型
    def fileNameFix = [".java", ".xml"] as String[]

    @Override
    void apply(Project project) {
        project.extensions.create("lintConfig", LintConfig.class)

        // build配置好之后再运行才能获取到extensions配置的数据
        project.afterEvaluate {
            println(project.lintConfig.forbidHook)

            CreateGitHooksUtils.createGitHooksFromResourcesTask(project, project.lintConfig.forbidHook, hookFileName)

            LintCheckUtils.lintCheck(project, fileNameFix.toList())
        }
    }
}

class LintConfig {
    def lintCheckFileType = ""
    def lintReportAll = false
    def forbidHook = false
}