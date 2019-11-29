package com.yong.lintrules;

import com.android.ddmlib.Log;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.ApiKt;
import com.android.tools.lint.detector.api.Issue;
import com.yong.lintrules.detectors.ColorParseDetector;
import com.yong.lintrules.detectors.SerializableDetector;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

public class IssuesRegister extends IssueRegistry {

    @NotNull
    @Override
    public List<Issue> getIssues() {
        Log.d("xiayong", "==== my lint start ====");
        Log.d("xiayong","api=" + getApi() + ",minApi=" + getMinApi()+",CurrentApi="+ ApiKt.CURRENT_API);

        return new ArrayList<Issue>() {{
            add(ColorParseDetector.ISSUE);
            add(SerializableDetector.ISSUE);
        }};
    }

    @Override
    public int getApi() {
        return ApiKt.CURRENT_API;
    }

    @Override
    public int getMinApi() {  //兼容3.1
        return 1;
    }
}
