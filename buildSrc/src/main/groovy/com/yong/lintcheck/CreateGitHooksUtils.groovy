import org.gradle.api.GradleException
import org.gradle.api.Project

class CreateGitHooksUtils {

    /**
     * 复制git hook脚本installGitHooks，finalizedBy保证它在build任务后面自动执行，
     * 它会把project工程git-hooks文件夹下的hookFileName文件复制到project工程.git/hooks文件夹下。
     * chmod -R +x .git/hooks/一定要写，不然没有权限
     * @param project
     */
    static void createGitHooksTask(Project project, boolean forbidHook, String hookFileName) {
        def preBuild = project.tasks.findByName("preBuild")

        if (preBuild == null) {
            throw new GradleException("lint need depend on preBuild and clean task")
            return
        }

        def installGitHooks = project.getTasks().create("installGitHooks")
                .doLast {

            println("=========== installGitHooks start ==============")

            File hookFile
            hookFile = new File(project.rootProject.rootDir, "git-hooks/" + hookFileName)

            if (forbidHook){

                File gitHookFile = new File(project.rootProject.rootDir, ".git/hooks/" + hookFileName)

                if (gitHookFile.exists()){
                    gitHookFile.delete()

                    println(".git hook " + hookFileName + " delete success")
                }
            }else {

                if (hookFile.exists()){
                    project.copy {
                        from (hookFile) {
                            rename {
                                String filename ->
                                    hookFileName
                            }
                        }
                        into new File(project.rootProject.rootDir, ".git/hooks/")
                    }
                    Runtime.getRuntime().exec("chmod -R +x .git/hooks/")

                    println("git hook " + hookFileName + " copy success")
                }else {
                    throw Exception("please ensure ${project.rootProject.rootDir}/git-hooks/" + hookFileName + "文件存在")
                }
            }

            println("=========== installGitHooks end ==============")
        }

        preBuild.finalizedBy installGitHooks
    }

    /**
     * 复制git hook脚本installGitHooks，finalizedBy保证它在build任务后面自动执行，
     * 它会把LintCheckUtils.class类所在工程的resources资源文件夹下的
     * hookFileName文件复制到整个工程的.git/hooks文件夹下。
     * chmod -R +x .git/hooks/一定要写，不然没有权限
     * @param project
     */
    static void createGitHooksFromResourcesTask(Project project, boolean forbidHook, String hookFileName){
        def preBuild = project.tasks.findByName("preBuild")

        if (preBuild == null) {
            throw new GradleException("lint  need depend on preBuild and clean task")
            return
        }

        def installGitHooks = project.getTasks().create("installGitHooks")
                .doLast {

            println("=========== installGitHooks start ==============")

            File hookFile = new File(project.rootProject.rootDir, ".git/hooks/" + hookFileName)
            if (!forbidHook) {
                if (copyResourceFile(hookFileName, hookFile)){
                    println("git hook " + hookFileName + " copy success")
                }else {
                    throw Exception("please ensure resources/git-hooks/" + hookFileName + "文件存在")
                }
            } else {
                if (hookFile.exists()) {
                    hookFile.delete()

                    println(".git hook " + hookFileName + " delete success")
                }
            }
            Runtime.getRuntime().exec("chmod -R +x .git/hooks/")

            println("=========== installGitHooks end ==============")
        }

        preBuild.finalizedBy installGitHooks
    }

    private static boolean copyResourceFile(String name, File dest) {
        FileOutputStream os = null
        File parent = dest.getParentFile()
        if (parent != null && (!parent.exists())) {
            parent.mkdirs()
        }
        InputStream is = null

        try {
            is = CreateGitHooksUtils.class.getClassLoader().getResourceAsStream("git-hooks/" + name)
            os = new FileOutputStream(dest, false)

            byte[] buffer = new byte[1024]
            int length
            while ((length = is.read(buffer)) > 0) {
                os.write(buffer, 0, length)
            }
            return true
        } catch (Exception e){
            e.printStackTrace()

            return false
        }finally {
            if (is != null) {
                is.close()
            }
            if (os != null) {
                os.close()
            }
        }
    }
}