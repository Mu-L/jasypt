/*
 * =============================================================================
 * 
 *   Copyright (c) 2007-2010, The JASYPT team (http://www.jasypt.org)
 * 
 *   Licensed under the Apache License, Version 2.0 (the "License");
 *   you may not use this file except in compliance with the License.
 *   You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *   Unless required by applicable law or agreed to in writing, software
 *   distributed under the License is distributed on an "AS IS" BASIS,
 *   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *   See the License for the specific language governing permissions and
 *   limitations under the License.
 * 
 * =============================================================================
 */
package org.jasypt.digest;


import java.util.concurrent.atomic.AtomicInteger;

import junit.framework.TestCase;

import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.time.StopWatch;

public class PooledStandardStringDigesterThreadedTest extends TestCase {

    private int numThreads = 20;
    private int numIters = 1000;
    protected int poolSize = 2;
    
    
    public PooledStandardStringDigesterThreadedTest() {
        super();
    }
    
    public PooledStandardStringDigesterThreadedTest(final int numThreads, final int numIters, final int poolSize) {
        super();
        this.numThreads = numThreads;
        this.numIters = numIters;
        this.poolSize = poolSize;
    }
    
    
    public void testThreadedDigest() throws Exception {
        TesterLauncher launcher = new TesterLauncher();
        assertTrue(launcher.launch(this.numThreads,this.numIters) == 0);
    }
    
    
    protected class TesterLauncher {

        private AtomicInteger runningThreads = null;
        private int numThreads = 0;
        
        public int launch(int numOfThreads, int numIters) throws Exception {
            
            this.numThreads = numOfThreads;
            
            PooledStringDigester digester = new PooledStringDigester();
            digester.setPoolSize(poolSize);
            AtomicInteger errors = new AtomicInteger(0);
            this.runningThreads = new AtomicInteger(0);
            
            for (int i = 0; i < numOfThreads; i++) {
                TesterRunnable tester = 
                    new TesterRunnable(digester, numIters, errors, 
                            this.runningThreads, this);
                Thread testerThread = new Thread(tester);
                testerThread.start();
            }
            
            while (continueWaiting()) {
                synchronized (this) {
                    this.wait(numIters * 1000);
                }
            }

            return errors.get();
            
        }
        
        private synchronized boolean continueWaiting() {
            return (this.runningThreads.get() < this.numThreads);
        }
        
    }
    
    
    private class TesterRunnable implements Runnable {

        private StringDigester digester = null;
        private int numIters = 0;
        private String message = null;
        private AtomicInteger errors = null;
        private AtomicInteger finishedThreads = null;
        private TesterLauncher launcher = null;
        
        public TesterRunnable(StringDigester digester, int numIters,  
                AtomicInteger errors, AtomicInteger finishedThreads,
                TesterLauncher launcher) {
            this.digester = digester;
            this.numIters = numIters;
            this.message = RandomStringUtils.randomAscii(20);
            this.errors = errors;
            this.finishedThreads = finishedThreads;
            this.launcher = launcher;
        }
        
        public void run() {
            
            int localErrors = 0;
            for (int i = 0; i < this.numIters; i++) {
                try {
                    String encryptedMessage = this.digester.digest(this.message);
                    if (!this.digester.matches(this.message, encryptedMessage)) {
                        localErrors++;
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    localErrors++;
                }
            }

            synchronized (this.launcher) {
                if (localErrors > 0) {
                    this.errors.addAndGet(localErrors);
                }
                this.finishedThreads.incrementAndGet();
                this.launcher.notify();
            }
        }
        
        
    }
    
    
    public static void main(String[] args) {
        try {
            
            final int numThreads = Integer.valueOf(args[0]).intValue();
            final int numIters = Integer.valueOf(args[1]).intValue();
            final int poolSize = Integer.valueOf(args[2]).intValue();
            
            PooledStandardStringDigesterThreadedTest test = 
                new PooledStandardStringDigesterThreadedTest(numThreads, numIters, poolSize);
            
            System.out.println("Starting test. NumThreads: " + numThreads + " NumIters: " + numIters + " PoolSize: " + poolSize);
            StopWatch sw = new StopWatch();
            sw.start();
            test.testThreadedDigest();
            sw.stop();
            System.out.println("Test finished in: " + sw.toString());
            
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
}
