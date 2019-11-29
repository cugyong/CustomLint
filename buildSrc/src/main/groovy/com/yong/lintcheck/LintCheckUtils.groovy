import com.android.tools.lint.XmlReporter
import com.yong.lintcheck.LintToolClient
import com.yong.lintrules.IssuesRegister
import com.yong.lintcheck.LintTxtReporter
import org.gradle.api.Project

class LintCheckUtils {

    /**
     * 通过Git命令获取本次提交的文件
     *
     * @param project gradle.Project
     * @return 文件名
     */
    private static List<String> getCommitChange(Project project) {
        ArrayList<String> filterList = new ArrayList<>()
        try {
            //此命令获取本次提交的文件 在git commit之后执行
            String command = "git diff --name-only --diff-filter=ACMRTUXB HEAD~1 HEAD~0"
            String changeInfo = command.execute(null, project.getRootDir()).text.trim()
            if (changeInfo == null || changeInfo.empty) {
                return filterList
            }

            String[] lines = changeInfo.split("\\n")
            return lines.toList()
        } catch (Exception e) {
            e.printStackTrace()
            return filterList
        }
    }

    /**
     * lint 代码扫描
     * @param project
     * @param defaultFilesType lint将要扫描的文件默认类型
     */
    static void lintCheck(Project project, List<String> defaultFilesType){
        project.task("lintCheck") << {

            /*
             * 输出TXT格式的报告
             */
            File lintResult = new File("lint-check-result.txt")
            Writer writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(lintResult), "UTF-8"))

            writer.write("=========== Lint check start ==============\n\n")

            /**
             * 解析lint将要扫描的文件类型
             */
            writer.write("Lint check file type: ")
            List<String> fileNamePostfix = getLintCheckFileType(project, defaultFilesType)
            for (int i = 0; i < fileNamePostfix.size(); i++){
                if (i == fileNamePostfix.size() - 1){
                    writer.write(fileNamePostfix.get(i) + "\n\n")
                }else {
                    writer.write(fileNamePostfix.get(i) + ", ")
                }
            }

            /**
             * 获取本次提交的文件
             */
            writer.write("=== git commit files list start ===\n")
            List<String> commitFileList = LintCheckUtils.getCommitChange(project)
            for (String str: commitFileList){
                writer.write(str)
                writer.newLine()
            }
            writer.write("=== git commit files list end ===\n\n")

            /**
             * 获取需要lint check的文件以及每个文件的修改行号
             */
            writer.write("=== lint check files list start ===\n")
            List<File> lintCheckFileList = new ArrayList<>()
            List<Integer> startIndex = new ArrayList<>()
            List<Integer> endIndex = new ArrayList<>()
            getLintCheckFilesInfo(project, commitFileList, fileNamePostfix,
                    lintCheckFileList, startIndex, endIndex)
            for (String str: lintCheckFileList){
                writer.write(str)
                writer.newLine()
            }
            writer.write("=== lint check files list end ===\n")

            /**
             * 开始扫描
             */
            def lintClient = new LintToolClient()
            def flags = lintClient.flags // LintCliFlags 用于设置Lint检查的一些标志
            // Whether lint should set the exit code of the process if errors are found
            flags.setExitCode = true

            //是否输出全部的扫描结果
            if (project.lintConfig != null && project.lintConfig.lintReportAll) {
                File outputResult = new File("lint-check-result-all.xml")
                def xmlReporter = new XmlReporter(cl, outputResult)
                flags.reporters.add(xmlReporter)
            }

            def txtReporter = new LintTxtReporter(lintClient, lintResult, writer, startIndex, endIndex)
            flags.reporters.add(txtReporter)

            lintClient.run(new IssuesRegister(), lintCheckFileList)

            //根据报告中存在的问题进行判断是否需要回退
            if (txtReporter.issueNumber > 0) {
                //回退commit
                "git reset HEAD~1".execute(null, project.getRootDir())
            }

            writer.newLine()
            writer.newLine()
            writer.write("=========== Lint check end ==============")

            writer.flush()
            writer.close()
        }
    }

    /**
     * 解析lint将要扫描的文件类型
     */
    private static List<String> getLintCheckFileType(Project project, List<String> defaultFilesType){
        List<String> fileNamePostfix
        String fileType = project.lintConfig.lintCheckFileType
        if (fileType != null && fileType != ""){
            fileNamePostfix = fileType.split(",").toList()
        }
        if (fileNamePostfix == null || fileNamePostfix.size() <= 0) {
            fileNamePostfix = defaultFilesType
        }
        return fileNamePostfix
    }

    /**
     * 通过git diff获取已提交文件的修改,包括文件的添加行的行号、删除行的行号、修改行的行号
     *
     * @param filePath 文件路径
     * @param project Project对象
     * @param startIndex 修改开始的下表数组
     * @param endIndex 修改结束的下表数组
     */
    private static void getFileChangeStatus(String filePath, Project project,
                                            List<Integer> startIndex, List<Integer> endIndex) {
        try {
            String command = "git diff --unified=0 --ignore-blank-lines --ignore-all-space HEAD~1 HEAD " + filePath
            String changeInfo = command.execute(null, project.getRootDir()).text.trim()
            String[] changeLogs = changeInfo.split("@@")
            String[] indexArray

            for (int i = 1; i < changeLogs.size(); i += 2) {
                indexArray = changeLogs[i].trim().split(" ")
                try {
                    int start, end
                    String[] startArray = null
                    if (indexArray.length > 1) {
                        startArray = indexArray[1].split(",")
                    }

                    if (startArray != null && startArray.length > 1) {
                        start = Integer.parseInt(startArray[0])
                        end = Integer.parseInt(startArray[0]) + Integer.parseInt(startArray[1])
                    } else {
                        start = Integer.parseInt(startArray[0])
                        end = start + 1
                    }
                    startIndex.add(start)
                    endIndex.add(end)
                } catch (NumberFormatException e) {
                    e.printStackTrace()
                    startIndex.add(0)
                    endIndex.add(0)
                }

            }
        } catch (Exception e) {
            e.printStackTrace()
        }
    }

    /**
     * 检查特定后缀的文件
     * 比如: .java .xml等
     *
     * @param fileName 文件名
     * @return 匹配 返回true 否则 返回 false
     */
    private static boolean isMatchFile(List<String> fileNamePostfix, String fileName) {
        for (String fix : fileNamePostfix) {
            if (fileName.endsWith(fix)) {
                return true
            }
        }
        return false
    }

    /**
     * 获取需要lint check的文件以及每个文件的修改行号
     * @param project
     * @param commitFileList
     * @param fileNamePostfix
     * @param lintCheckFileList
     * @param startIndex
     * @param endIndex
     */
    private static void getLintCheckFilesInfo(Project project,
                                              List<String> commitFileList,
                                              List<String> fileNamePostfix,
                                              List<File> lintCheckFileList,
                                              List<Integer> startIndex,
                                              List<Integer> endIndex){
        for (String fileName : commitFileList) {
            if (isMatchFile(fileNamePostfix, fileName)) {
                File file = new File(fileName)
                lintCheckFileList.add(file)
                getFileChangeStatus(fileName, project, startIndex, endIndex)
            }
        }
    }
}