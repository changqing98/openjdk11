/*
 * Copyright (c) 2001, 2018, Oracle and/or its affiliates. All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License version 2 only, as
 * published by the Free Software Foundation.
 *
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE.  See the GNU General Public License
 * version 2 for more details (a copy is included in the LICENSE file that
 * accompanied this code).
 *
 * You should have received a copy of the GNU General Public License version
 * 2 along with this work; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 * Please contact Oracle, 500 Oracle Parkway, Redwood Shores, CA 94065 USA
 * or visit www.oracle.com if you need additional information or have any
 * questions.
 */

package nsk.jdi.EventSet.resume;

import nsk.share.*;
import nsk.share.jpda.*;
import nsk.share.jdi.*;

import com.sun.jdi.*;
import com.sun.jdi.event.*;
import com.sun.jdi.request.*;

import java.util.*;
import java.io.*;

/**
 * The test for the implementation of an object of the type     <BR>
 * EventSet.                                                    <BR>
 *                                                              <BR>
 * The test checks that results of the method                   <BR>
 * <code>com.sun.jdi.EventSet.resume()</code>                   <BR>
 * complies with its spec.                                      <BR>
 * <BR>
 * Test cases include all three possible suspensions, NONE,     <BR>
 * EVENT_THREAD, and ALL, for MethodEntryEvent sets.            <BR>
 * <BR>
 * To check up on the method, a debugger,                       <BR>
 * upon getting new set for the EventSet,                       <BR>
 * suspends VM with the method VirtualMachine.suspend(),        <BR>
 * gets the List of geduggee's threads calling VM.allThreads(), <BR>
 * invokes the method EventSet.resume(), and                    <BR>
 * gets another List of geduggee's threads.                     <BR>
 * The debugger then compares values of                         <BR>
 * each thread's suspendCount from first and second Lists.      <BR>
 * <BR>
 * The test has three phases and works as follows.              <BR>
 * <BR>
 * In first phase,                                                      <BR>
 * upon launching debuggee's VM which will be suspended,                <BR>
 * a debugger waits for the VMStartEvent within a predefined            <BR>
 * time interval. If no the VMStartEvent received, the test is FAILED.  <BR>
 * Upon getting the VMStartEvent, it makes the request for debuggee's   <BR>
 * ClassPrepareEvent with SUSPEND_EVENT_THREAD, resumes the VM,         <BR>
 * and waits for the event within the predefined time interval.         <BR>
 * If no the ClassPrepareEvent received, the test is FAILED.            <BR>
 * Upon getting the ClassPrepareEvent,                                  <BR>
 * the debugger sets up the breakpoint with SUSPEND_EVENT_THREAD        <BR>
 * within debuggee's special methodForCommunication().                  <BR>
 * <BR>
 * In second phase to check the above,                                  <BR>
 * the debugger and the debuggee perform the following loop.            <BR>
 * - The debugger sets up a MethodEntryRequest, resumes                 <BR>
 *   the debuggee, and waits for the MethodEntryEvent.                  <BR>
 * - The debuggee invokes the special method which makes access to      <BR>
 *   a predefined variable to be resulting in the event.                <BR>
 * - Upon getting new event, the debugger                               <BR>
 *   performs the check corresponding to the event.                     <BR>
 * <BR>
 * Note. To inform each other of needed actions, the debugger and       <BR>
 *       and the debuggee use debuggee's variable "instruction".        <BR>
 * In third phase, at the end,                                          <BR>
 * the debuggee changes the value of the "instruction"                  <BR>
 * to inform the debugger of checks finished, and both end.             <BR>
 * <BR>
 */

public class resume006 {

    //----------------------------------------------------- templete section
    static final int PASSED = 0;
    static final int FAILED = 2;
    static final int PASS_BASE = 95;

    //----------------------------------------------------- templete parameters
    static final String
    sHeader1 = "\n==> nsk/jdi/EventSet/resume/resume006 ",
    sHeader2 = "--> debugger: ",
    sHeader3 = "##> debugger: ";

    //----------------------------------------------------- main method

    public static void main (String argv[]) {

        int result = run(argv, System.out);

        System.exit(result + PASS_BASE);
    }

    public static int run (String argv[], PrintStream out) {

        int exitCode = new resume006().runThis(argv, out);

        if (exitCode != PASSED) {
            System.out.println("TEST FAILED");
        }
        return testExitCode;
    }

    //--------------------------------------------------   log procedures

    private static Log  logHandler;

    private static void log1(String message) {
        logHandler.display(sHeader1 + message);
    }
    private static void log2(String message) {
        logHandler.display(sHeader2 + message);
    }
    private static void log3(String message) {
        logHandler.complain(sHeader3 + message);
    }

    //  ************************************************    test parameters

    private String debuggeeName =
        "nsk.jdi.EventSet.resume.resume006a";

    private String testedClassName =
      "nsk.jdi.EventSet.resume.resume006aTestClass";

    //====================================================== test program
    //------------------------------------------------------ common section

    static Debugee          debuggee;
    static ArgumentHandler  argsHandler;

    static int waitTime;

    static VirtualMachine      vm            = null;
    static EventRequestManager eventRManager = null;
    static EventQueue          eventQueue    = null;
    static EventSet            eventSet      = null;
    static EventIterator       eventIterator = null;

    static ReferenceType       debuggeeClass = null;

    static int  testExitCode = PASSED;


    class JDITestRuntimeException extends RuntimeException {
        JDITestRuntimeException(String str) {
            super("JDITestRuntimeException : " + str);
        }
    }

    //------------------------------------------------------ methods

    private int runThis (String argv[], PrintStream out) {

        argsHandler     = new ArgumentHandler(argv);
        logHandler      = new Log(out, argsHandler);
        Binder binder   = new Binder(argsHandler, logHandler);

        waitTime        = argsHandler.getWaitTime() * 60000;

        try {
            log2("launching a debuggee :");
            log2("       " + debuggeeName);
            if (argsHandler.verbose()) {
                debuggee = binder.bindToDebugee(debuggeeName + " -vbs");
            } else {
                debuggee = binder.bindToDebugee(debuggeeName);
            }
            if (debuggee == null) {
                log3("ERROR: no debuggee launched");
                return FAILED;
            }
            log2("debuggee launched");
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            log2("       test cancelled");
            return FAILED;
        }

        debuggee.redirectOutput(logHandler);

        vm = debuggee.VM();

        eventQueue = vm.eventQueue();
        if (eventQueue == null) {
            log3("ERROR: eventQueue == null : TEST ABORTED");
            vm.exit(PASS_BASE);
            return FAILED;
        }

        log2("invocation of the method runTest()");
        switch (runTest()) {

            case 0 :  log2("test phase has finished normally");
                      log2("   waiting for the debuggee to finish ...");
                      debuggee.waitFor();

                      log2("......getting the debuggee's exit status");
                      int status = debuggee.getStatus();
                      if (status != PASS_BASE) {
                          log3("ERROR: debuggee returned UNEXPECTED exit status: " +
                              status + " != PASS_BASE");
                          testExitCode = FAILED;
                      } else {
                          log2("......debuggee returned expected exit status: " +
                              status + " == PASS_BASE");
                      }
                      break;

            default : log3("ERROR: runTest() returned unexpected value");

            case 1 :  log3("test phase has not finished normally: debuggee is still alive");
                      log2("......forcing: vm.exit();");
                      testExitCode = FAILED;
                      try {
                          vm.exit(PASS_BASE);
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : e");
                      }
                      break;

            case 2 :  log3("test cancelled due to VMDisconnectedException");
                      log2("......trying: vm.process().destroy();");
                      testExitCode = FAILED;
                      try {
                          Process vmProcess = vm.process();
                          if (vmProcess != null) {
                              vmProcess.destroy();
                          }
                      } catch ( Exception e ) {
                          log3("ERROR: Exception : e");
                      }
                      break;
            }

        return testExitCode;
    }


   /*
    * Return value: 0 - normal end of the test
    *               1 - ubnormal end of the test
    *               2 - VMDisconnectedException while test phase
    */

    private int runTest() {

        try {
            testRun();

            log2("waiting for VMDeathEvent");
            getEventSet();
            if (eventIterator.nextEvent() instanceof VMDeathEvent)
                return 0;

            log3("ERROR: last event is not the VMDeathEvent");
            return 1;
        } catch ( VMDisconnectedException e ) {
            log3("ERROR: VMDisconnectedException : " + e);
            return 2;
        } catch ( Exception e ) {
            log3("ERROR: Exception : " + e);
            return 1;
        }

    }

    private void testRun()
                 throws JDITestRuntimeException, Exception {

        eventRManager = vm.eventRequestManager();

        ClassPrepareRequest cpRequest = eventRManager.createClassPrepareRequest();
        cpRequest.setSuspendPolicy( EventRequest.SUSPEND_EVENT_THREAD);
        cpRequest.addClassFilter(debuggeeName);

        cpRequest.enable();
        vm.resume();
        getEventSet();
        cpRequest.disable();

        ClassPrepareEvent event = (ClassPrepareEvent) eventIterator.next();
        debuggeeClass = event.referenceType();

        if (!debuggeeClass.name().equals(debuggeeName))
           throw new JDITestRuntimeException("** Unexpected ClassName for ClassPrepareEvent **");

        log2("      received: ClassPrepareEvent for debuggeeClass");

        String bPointMethod = "methodForCommunication";
        String lineForComm  = "lineForComm";
        BreakpointRequest bpRequest;
        ThreadReference   mainThread = threadByName("main");
        bpRequest = settingBreakpoint(mainThread,
                                      debuggeeClass,
                                      bPointMethod, lineForComm, "zero");
        bpRequest.enable();

        vm.resume();

    //------------------------------------------------------  testing section

        log1("     TESTING BEGINS");

        EventRequest eventRequest1 = null;
        EventRequest eventRequest2 = null;
        EventRequest eventRequest3 = null;

        final int SUSPEND_NONE   = EventRequest.SUSPEND_NONE;
        final int SUSPEND_THREAD = EventRequest.SUSPEND_EVENT_THREAD;
        final int SUSPEND_ALL    = EventRequest.SUSPEND_ALL;

        ReferenceType testClassReference = null;


        for (int i = 0; ; i++) {

            breakpointForCommunication();

            int instruction = ((IntegerValue)
                               (debuggeeClass.getValue(debuggeeClass.fieldByName("instruction")))).value();

            if (instruction == 0) {
                vm.resume();
                break;
            }

            log1(":::::: case: # " + i);

            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~ variable part

            switch (i) {

              case 0:
                      testClassReference =
                           (ReferenceType) vm.classesByName(testedClassName).get(0);

                      eventRequest1 = settingMethodEntryRequest (mainThread,
                                             testClassReference,
                                             SUSPEND_NONE, "MethodEntryRequest1");
                      eventRequest1.enable();
                      break;

              case 1:
                      eventRequest2 = settingMethodEntryRequest (mainThread,
                                             testClassReference,
                                             SUSPEND_THREAD, "MethodEntryRequest2");
                      eventRequest2.enable();
                      break;

              case 2:
                      eventRequest3 = settingMethodEntryRequest (mainThread,
                                             testClassReference,
                                             SUSPEND_ALL, "MethodEntryRequest3");
                      eventRequest3.enable();
                      break;


              default:
                      throw new JDITestRuntimeException("** default case 2 **");
            }

            log2("......waiting for new MethodEntryEvent : " + i);
            mainThread.resume();
            getEventSet();

            Event newEvent = eventIterator.nextEvent();
            if ( !(newEvent instanceof MethodEntryEvent)) {
                 log3("ERROR: new event is not MethodEntryEvent");
                 testExitCode = FAILED;
            } else {

                String property = (String) newEvent.request().getProperty("number");
                log2("       got new MethodEntryEvent with propety 'number' == " + property);

                log2("......checking up on EventSet.resume()");
                log2("......--> vm.suspend();");
                vm.suspend();

                log2("        getting : Map<String, Integer> suspendsCounts1");

                Map<String, Integer> suspendsCounts1 = new HashMap<String, Integer>();
                for (ThreadReference threadReference : vm.allThreads()) {
                    suspendsCounts1.put(threadReference.name(), threadReference.suspendCount());
                }

                log2("        eventSet.resume;");
                eventSet.resume();

                log2("        getting : Map<String, Integer> suspendsCounts2");
                Map<String, Integer> suspendsCounts2 = new HashMap<String, Integer>();
                for (ThreadReference threadReference : vm.allThreads()) {
                    suspendsCounts2.put(threadReference.name(), threadReference.suspendCount());
                }

                log2(suspendsCounts1.toString());
                log2(suspendsCounts2.toString());

                log2("        getting : int policy = eventSet.suspendPolicy();");
                int policy = eventSet.suspendPolicy();

                switch (policy) {

                  case SUSPEND_NONE   :
                       log2("        case SUSPEND_NONE");
                       for (String threadName : suspendsCounts1.keySet()) {
                           log2("        checking " + threadName);
                           if (!suspendsCounts2.containsKey(threadName)) {
                               log3("ERROR: couldn't get ThreadReference for " + threadName);
                               testExitCode = FAILED;
                               break;
                           }
                           int count1 = suspendsCounts1.get(threadName);
                           int count2 = suspendsCounts2.get(threadName);
                           if (count1 != count2) {
                               log3("ERROR: suspendCounts don't match for : " + threadName);
                               log3("before resuming : " + count1);
                               log3("after  resuming : " + count2);
                               testExitCode = FAILED;
                               break;
                           }
                       }
                       eventRequest1.disable();
                       break;

                  case SUSPEND_THREAD :
                       log2("        case SUSPEND_THREAD");
                       for (String threadName : suspendsCounts1.keySet()) {
                           log2("checking " + threadName);
                           if (!suspendsCounts2.containsKey(threadName)) {
                               log3("ERROR: couldn't get ThreadReference for " + threadName);
                               testExitCode = FAILED;
                               break;
                           }
                           int count1 = suspendsCounts1.get(threadName);
                           int count2 = suspendsCounts2.get(threadName);
                           String eventThreadName = event.thread().name();
                           int expectedValue = count2 + (eventThreadName.equals(threadName) ? 1 : 0);
                           if (count1 != expectedValue) {
                               log3("ERROR: suspendCounts don't match for : " + threadName);
                               log3("before resuming : " + count1);
                               log3("after  resuming : " + count2);
                               testExitCode = FAILED;
                               break;
                           }
                       }
                       eventRequest2.disable();
                       break;

                  case SUSPEND_ALL    :

                        log2("        case SUSPEND_ALL");
                        for (String threadName : suspendsCounts1.keySet()) {
                            log2("checking " + threadName);
                            if (!event.request().equals(eventRequest3))
                                break;
                            if (!suspendsCounts2.containsKey(threadName)) {
                                log3("ERROR: couldn't get ThreadReference for " + threadName);
                                testExitCode = FAILED;
                                break;
                            }
                            int count1 = suspendsCounts1.get(threadName);
                            int count2 = suspendsCounts2.get(threadName);
                            if (count1 != count2 + 1) {
                                log3("ERROR: suspendCounts don't match for : " + threadName);
                                log3("before resuming : " + count1);
                                log3("after  resuming : " + count2);
                                testExitCode = FAILED;
                                break;
                            }
                        }
                        eventRequest3.disable();
                        break;


                  default: throw new JDITestRuntimeException("** default case 1 **");
                }
            }

            log2("......--> vm.resume()");
            vm.resume();
            //~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~
        }
        log1("    TESTING ENDS");
        return;
    }

    private ThreadReference threadByName(String name)
                 throws JDITestRuntimeException {

        List         all = vm.allThreads();
        ListIterator li  = all.listIterator();

        for (; li.hasNext(); ) {
            ThreadReference thread = (ThreadReference) li.next();
            if (thread.name().equals(name))
                return thread;
        }
        throw new JDITestRuntimeException("** Thread IS NOT found ** : " + name);
    }

   /*
    * private BreakpointRequest settingBreakpoint(ThreadReference, ReferenceType,
    *                                             String, String, String)
    *
    * It sets up a breakpoint at given line number within a given method in a given class
    * for a given thread.
    *
    * Return value: BreakpointRequest object  in case of success
    *
    * JDITestRuntimeException   in case of an Exception thrown within the method
    */

    private BreakpointRequest settingBreakpoint ( ThreadReference thread,
                                                  ReferenceType testedClass,
                                                  String methodName,
                                                  String bpLine,
                                                  String property)
            throws JDITestRuntimeException {

        log2("......setting up a breakpoint:");
        log2("       thread: " + thread + "; class: " + testedClass +
                        "; method: " + methodName + "; line: " + bpLine);

        List              alllineLocations = null;
        Location          lineLocation     = null;
        BreakpointRequest breakpRequest    = null;

        try {
            Method  method  = (Method) testedClass.methodsByName(methodName).get(0);

            alllineLocations = method.allLineLocations();

            int n =
                ( (IntegerValue) testedClass.getValue(testedClass.fieldByName(bpLine) ) ).value();
            if (n > alllineLocations.size()) {
                log3("ERROR:  TEST_ERROR_IN_settingBreakpoint(): number is out of bound of method's lines");
            } else {
                lineLocation = (Location) alllineLocations.get(n);
                try {
                    breakpRequest = eventRManager.createBreakpointRequest(lineLocation);
                    breakpRequest.putProperty("number", property);
                    breakpRequest.addThreadFilter(thread);
                    breakpRequest.setSuspendPolicy( EventRequest.SUSPEND_EVENT_THREAD);
                } catch ( Exception e1 ) {
                    log3("ERROR: inner Exception within settingBreakpoint() : " + e1);
                    breakpRequest    = null;
                }
            }
        } catch ( Exception e2 ) {
            log3("ERROR: ATTENTION:  outer Exception within settingBreakpoint() : " + e2);
            breakpRequest    = null;
        }

        if (breakpRequest == null) {
            log2("      A BREAKPOINT HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("**FAILURE to set up a breakpoint**");
        }

        log2("      a breakpoint has been set up");
        return breakpRequest;
    }


    private void getEventSet()
                 throws JDITestRuntimeException {
        try {
//            log2("       eventSet = eventQueue.remove(waitTime);");
            eventSet = eventQueue.remove(waitTime);
            if (eventSet == null) {
                throw new JDITestRuntimeException("** TIMEOUT while waiting for event **");
            }
//            log2("       eventIterator = eventSet.eventIterator;");
            eventIterator = eventSet.eventIterator();
        } catch ( Exception e ) {
            throw new JDITestRuntimeException("** EXCEPTION while waiting for event ** : " + e);
        }
    }


    private void breakpointForCommunication()
                 throws JDITestRuntimeException {

        log2("breakpointForCommunication");
        getEventSet();

        if (eventIterator.nextEvent() instanceof BreakpointEvent)
            return;

        throw new JDITestRuntimeException("** event IS NOT a breakpoint **");
    }

    // ============================== test's additional methods

    private MethodEntryRequest settingMethodEntryRequest ( ThreadReference thread,
                                                           ReferenceType   testedClass,
                                                           int             suspendPolicy,
                                                           String          property       )
            throws JDITestRuntimeException {
        try {
            log2("......setting up MethodEntryRequest:");
            log2("       thread: " + thread + "; class: " + testedClass +  "; property: " + property);

            MethodEntryRequest
            menr = eventRManager.createMethodEntryRequest();
            menr.putProperty("number", property);
            menr.addThreadFilter(thread);
            menr.addClassFilter(testedClass);
            menr.setSuspendPolicy(suspendPolicy);

            log2("      a MethodEntryRequest has been set up");
            return menr;
        } catch ( Exception e ) {
            log3("ERROR: ATTENTION: Exception within settingMethodEntryRequest() : " + e);
            log3("       MethodEntryRequest HAS NOT BEEN SET UP");
            throw new JDITestRuntimeException("** FAILURE to set up MethodEntryRequest **");
        }
    }

}
