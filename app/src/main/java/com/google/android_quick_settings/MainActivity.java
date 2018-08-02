// Copyright 2016 Google Inc.
// 
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
// 
//      http://www.apache.org/licenses/LICENSE-2.0
// 
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package com.google.android_quick_settings;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

import com.topjohnwu.superuser.Shell;

import java.util.List;

public class MainActivity extends AppCompatActivity {
    private Shell.Container container;
    private Thread thread = new Thread() {
        @Override
    public void run() {
        try {
            while (!thread.isInterrupted()) {
                Thread.sleep(1000);
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        // update TextView here!
                        doSomething();
                    }
                });
            }
        } catch (InterruptedException e) {
        }
    }
    };

    private View.OnClickListener awesomeOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            doSomething();
        }
    };


        @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assign the container with a pre-configured Container
        container = Shell.Config.newContainer();
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.mcsInfo);
        //textView.setOnClickListener(awesomeOnClickListener);
        thread.start();

    }

    protected void doSomething(){
        List<String> output = Shell.su("mcs -i").exec().getOut();

        TextView textView = (TextView) findViewById(R.id.mcsInfo);
        textView.setText("");
        for(String tmp:output){
            textView.append(tmp + "\n");
        }

    }

    @Override
    public void onRestart() {
        super.onRestart();
        doSomething();
    }
}
