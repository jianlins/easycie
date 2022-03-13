/*
 * Copyright  2017  Department of Biomedical Informatics, University of Utah
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package edu.utah.bmi.nlp.easycie.entry;

import edu.utah.bmi.nlp.core.IOUtil;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.RunnableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.logging.Logger;

/**
 * Extend Task  to open updateMessage and updateProgress methods.
 *
 * @author Jianlin Shi
 * Created on 2/24/17.
 */
public class EasyCIETask<V>  implements RunnableFuture<V> {
    public static Logger logger = IOUtil.getLogger(EasyCIETask.class);
    public boolean print = false;

    public EasyCIETask() {
    }


    public void updateGUIMessage(String msg) {
        logger.info(msg);
    }

    public void updateGUIProgress(Double workDone, Double max) {
        logger.info(workDone + "/" + max);
    }

    public void updateGUIProgress(Long workDone, Long max) {
        logger.info(workDone + "/" + max);
    }

    public void updateGUIProgress(int workDone, int max) {
        logger.info(workDone + "/" + max);
    }

    public void popDialog(String title, String header, String content) {
        logger.info(title + ":\t" + header + ":\t" + content);
    }

    public void guiCall() {
        try {
            this.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    protected Object call() throws Exception{
        return null;
    }


    @Override
    public void run() {
        try {
            this.call();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean cancel(boolean mayInterruptIfRunning) {
        return false;
    }

    @Override
    public boolean isCancelled() {
        return false;
    }

    @Override
    public boolean isDone() {
        return false;
    }

    @Override
    public V get() throws InterruptedException, ExecutionException {
        return null;
    }

    @Override
    public V get(long timeout, TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return null;
    }
}
