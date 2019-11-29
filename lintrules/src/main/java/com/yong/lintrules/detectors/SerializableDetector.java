package com.yong.lintrules.detectors;

import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Detector;
import com.android.tools.lint.detector.api.Implementation;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.JavaContext;
import com.android.tools.lint.detector.api.Scope;
import com.android.tools.lint.detector.api.Severity;
import com.intellij.psi.PsiClassType;

import org.jetbrains.annotations.Nullable;
import org.jetbrains.uast.UClass;

import java.util.Collections;
import java.util.List;

/**
 * @Description: 序列化类的内部类也必须序列化
 * @author: cugyong
 * @CreateDate: 2019/11/22 下午7:19
 * @UpdateUser: 更新者：
 * @UpdateDate: 2019/11/22 下午7:19
 * @UpdateRemark: 更新说明：
 */
public class SerializableDetector extends Detector implements Detector.UastScanner {

    private static final String CLASS_SERIALIZABLE = "java.io.Serializable";

    public static final Issue ISSUE = Issue.create(
            "InnerClassSerializable",
            "内部类需要实现Serializable接口",
            "内部类需要实现Serializable接口，否则会出错",
            Category.SECURITY, 6, Severity.ERROR,
            new Implementation(SerializableDetector.class, Scope.JAVA_FILE_SCOPE));

    @Nullable
    @Override
    public List<String> applicableSuperClasses() {
        // 继承自"java.io.Serializable"的类
        return Collections.singletonList(CLASS_SERIALIZABLE);
    }

    @Override
    public void visitClass(JavaContext context, UClass declaration) {
        visitInnerClass(context, declaration);
    }

    private void visitInnerClass(JavaContext context, UClass declaration){
        for (UClass uClass: declaration.getInnerClasses()){
            visitInnerClass(context, uClass);

            boolean hasImple = false;
            for (PsiClassType psiClassType: uClass.getImplementsListTypes()){
                if (CLASS_SERIALIZABLE.equals(psiClassType.getCanonicalText())){
                    hasImple = true;
                    break;
                }
            }

            if (!hasImple){
                context.report(ISSUE, uClass.getNameIdentifier(),
                        context.getLocation(uClass.getNameIdentifier()),
                        String.format("内部类 %s 需要实现Serializable接口", uClass.getName()));
            }
        }
    }
}
