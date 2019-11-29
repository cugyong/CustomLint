package com.yong.lintrules.detectors;

import com.android.tools.lint.client.api.JavaEvaluator;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiMethod;

import org.jetbrains.uast.UCallExpression;
import org.jetbrains.uast.UClass;
import org.jetbrains.uast.UastUtils;

import java.util.Collections;
import java.util.List;

/**
 * @Description: 颜色解析必须加上try catch
 * @author: cugyong
 * @CreateDate: 2019/11/19 下午2:07
 * @UpdateUser: 更新者：
 * @UpdateDate: 2019/11/19 下午2:07
 * @UpdateRemark: 更新说明：
 */
public class ColorParseDetector extends Detector implements Detector.UastScanner{

    public static final String ISSUE_ID = "ColorParse";

    public static final String ISSUE_DESCRIPTION = "避免 parseColor 解析出现异常";

    public static final String ISSUE_EXPLANATION = "当解析错误时会抛出异常，请加入try catch防护";

    public static final Category ISSUE_CATEGORY = Category.SECURITY;

    /**
     * 优先级，1到10的数字，10是最重要/最严重的
     */
    private static final int ISSUE_PRIORITY = 6;

    private static final Severity ISSUE_SEVERITY = Severity.ERROR;

    /**
     * 特定的方法名
     */
    static final String PARSECOLOR = "parseColor";

    public static final Issue ISSUE = Issue.create(
            ISSUE_ID,
            ISSUE_DESCRIPTION,
            ISSUE_EXPLANATION,
            ISSUE_CATEGORY,
            ISSUE_PRIORITY,
            ISSUE_SEVERITY,
            new Implementation(ColorParseDetector.class, Scope.JAVA_FILE_SCOPE)
    );

    @Override
    public List<String> getApplicableMethodNames() {
        return Collections.singletonList(PARSECOLOR);
    }

    @Override
    public void visitMethod(JavaContext context, UCallExpression node, PsiMethod method) {
        //如果方法名不一致就不走判断逻辑
        if (method.getName().equals(PARSECOLOR)) {
            JavaEvaluator evaluator = context.getEvaluator();
            //接着要确定是哪个类的方法
            if (evaluator.isMemberInClass(method, "android.graphics.Color")) {
                /**
                 * 在AST抽象语法树中，调用 parseColor 的节点应该是 try 的子节点，
                 * 向上追溯，查到的对应的是 Try 那么就说明已经在调用 parseColor 前做了try-catch处理
                 */
                UClass cls = UastUtils.getParentOfType(node, UClass.class, true);
                boolean hasTry = cls != null
                        && evaluator.extendsClass(
                        cls, "javaslang.control.Try", false);
                if (!hasTry) {
                    context.report(ISSUE,
                            context.getLocation(node),
                            ISSUE_EXPLANATION
                    );
                }
            }
        }
    }
}


