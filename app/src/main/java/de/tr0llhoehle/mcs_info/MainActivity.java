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

package de.tr0llhoehle.mcs_info;

import android.content.Context;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.method.ScrollingMovementMethod;
import android.view.Gravity;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import de.tr0llhoehle.mcs_info.R;
import com.topjohnwu.superuser.Shell;

import java.util.List;
import java.util.Locale;

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
                            printMCSInfo();
                        }
                    });
                }
            } catch (InterruptedException e) {
            }
        }
    };

    //currently not used
    private View.OnClickListener awesomeOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(View v) {
            printMCSInfo();
        }
    };


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Assign the container with a pre-configured Container
        if(container == null) {
            container = Shell.Config.newContainer();
        }
        setContentView(R.layout.activity_main);
        TextView textView = (TextView) findViewById(R.id.mcsInfo);
        //textView.setOnClickListener(awesomeOnClickListener);
        textView.setMovementMethod(new ScrollingMovementMethod());


        //Toast
        Context context = getApplicationContext();
        CharSequence text = String.format(Locale.US,
                "%s",
                getString(R.string.app_greeting) );
        int duration = Toast.LENGTH_LONG;
        Toast toast = Toast.makeText(context, text, duration);
        toast.setGravity(Gravity.TOP, 0, 0);
        toast.show();

        printMCSInfo();
        thread.start();


    }

    protected void printMCSInfo() {
        List<String> output = Shell.su("acc -i").exec().getOut();

        TextView textView = (TextView) findViewById(R.id.mcsInfo);
        textView.setText("");
        for (String tmp : output) {
            textView.append(tmp + "\n");
        }

    }

    @Override
    public void onRestart() {
        super.onRestart();
        printMCSInfo();
    }
}
