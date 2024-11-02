<!--
- Copyright (C) 2019 Ignite Realtime Foundation. All rights reserved.
-
- Licensed under the Apache License, Version 2.0 (the "License");
- you may not use this file except in compliance with the License.
- You may obtain a copy of the License at
-
- http://www.apache.org/licenses/LICENSE-2.0
-
- Unless required by applicable law or agreed to in writing, software
- distributed under the License is distributed on an "AS IS" BASIS,
- WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
- See the License for the specific language governing permissions and
- limitations under the License.
-->
<%@ page contentType="text/html; charset=UTF-8" %>
<%@ page errorPage="error.jsp" %>
<%@ page import="org.igniterealtime.openfire.plugin.threaddump.ThreadDump" %>
<%@ page import="org.igniterealtime.openfire.plugin.threaddump.ThreadDumpPlugin" %>
<%@ page import="org.igniterealtime.openfire.plugin.threaddump.formatter.DefaultThreadDumpFormatter" %>
<%@ page import="org.jivesoftware.openfire.XMPPServer" %>
<%@ page import="org.jivesoftware.util.CookieUtils" %>
<%@ page import="org.jivesoftware.util.JiveGlobals" %>
<%@ page import="org.jivesoftware.util.ParamUtils" %>
<%@ page import="org.jivesoftware.util.StringUtils" %>
<%@ page import="java.time.Duration" %>
<%@ page import="java.time.temporal.ChronoUnit" %>
<%@ page import="java.util.HashSet" %>
<%@ page import="java.util.Set" %>
<%@ page import="org.igniterealtime.openfire.plugin.threaddump.evaluator.*" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<jsp:useBean id="webManager" class="org.jivesoftware.util.WebManager"  />
<% webManager.init(request, response, session, application, out ); %>
<%
    final ThreadDumpPlugin plugin = (ThreadDumpPlugin) XMPPServer.getInstance().getPluginManager().getPlugin( "threaddump" );
    String success = request.getParameter("success");
    boolean update = request.getParameter("update") != null;

    String error = null;

    final Cookie csrfCookie = CookieUtils.getCookie( request, "csrf");
    String csrfParam = ParamUtils.getParameter( request, "csrf");

    if (update)
    {
        if ( csrfCookie == null || csrfParam == null || !csrfCookie.getValue().equals( csrfParam ) )
        {
            error = "csrf";
        }
        else
        {
            // First parse all properties. Allow exceptions to be thrown before any properties are changed.
            final boolean enableTask = request.getParameter( "enableTask" ) != null && ( request.getParameter( "enableTask" ).equals( "on" ) || request.getParameter( "enableTask" ).equals( "true" ) );
            final long taskDelayMS = Long.parseLong( request.getParameter( "taskDelayMS" ) );
            final long taskIntervalMS = Long.parseLong( request.getParameter( "taskIntervalMS" ) );
            final long taskBackoffMS = Long.parseLong( request.getParameter( "taskBackoffMS" ) );

            final boolean poolEvaluatorEnabled = request.getParameter( "poolEvaluatorEnabled" ) != null && ( request.getParameter( "poolEvaluatorEnabled" ).equals( "true" ) || request.getParameter( "poolEvaluatorEnabled" ).equals( "on" ) );
            final long poolEvaluatorIntervalMS = Long.parseLong( request.getParameter( "poolEvaluatorIntervalMS" ) );
            final int poolEvaluatorBusyPercentage = Integer.parseInt( request.getParameter( "poolEvaluatorBusyPercentage" ) );
            final int poolEvaluatorSuccessiveHits = Integer.parseInt( request.getParameter( "poolEvaluatorSuccessiveHits" ) );

            final boolean dbPoolEvaluatorEnabled = request.getParameter( "dbPoolEvaluatorEnabled" ) != null && ( request.getParameter( "dbPoolEvaluatorEnabled" ).equals( "true" ) || request.getParameter( "dbPoolEvaluatorEnabled" ).equals( "on" ) );
            final long dbPoolEvaluatorIntervalMS = Long.parseLong( request.getParameter( "dbPoolEvaluatorIntervalMS" ) );
            final int dbPoolEvaluatorBusyPercentage = Integer.parseInt( request.getParameter( "dbPoolEvaluatorBusyPercentage" ) );
            final int dbPoolEvaluatorSuccessiveHits = Integer.parseInt( request.getParameter( "dbPoolEvaluatorSuccessiveHits" ) );

            final boolean deadlockEvaluatorEnabled = request.getParameter( "deadlockEvaluatorEnabled" ) != null && ( request.getParameter( "deadlockEvaluatorEnabled" ).equals( "true" ) || request.getParameter( "deadlockEvaluatorEnabled" ).equals( "on" ) );
            final long deadlockEvaluatorIntervalMS = Long.parseLong( request.getParameter( "deadlockEvaluatorIntervalMS" ) );

            final boolean taskEngineEvaluatorEnabled = request.getParameter( "taskEngineEvaluatorEnabled" ) != null && ( request.getParameter( "taskEngineEvaluatorEnabled" ).equals( "true" ) || request.getParameter( "taskEngineEvaluatorEnabled" ).equals( "on" ) );
            final int taskEngineEvaluatorMaxThreads = Integer.parseInt( request.getParameter( "taskEngineEvaluatorMaxThreads" ) );
            final int taskEngineEvaluatorMaxPoolSize = Integer.parseInt( request.getParameter( "taskEngineEvaluatorMaxPoolSize" ) );
            final long taskEngineEvaluatorIntervalMS = Long.parseLong( request.getParameter( "taskEngineEvaluatorIntervalMS" ) );
            final int taskEngineEvaluatorSuccessiveHits = Integer.parseInt( request.getParameter( "taskEngineEvaluatorSuccessiveHits" ) );

            final Set<Class<? extends Evaluator>> enabledEvaluators = new HashSet<>();
            if ( poolEvaluatorEnabled ) {
                enabledEvaluators.add( CoreThreadPoolsEvaluator.class );
            }
            if ( dbPoolEvaluatorEnabled ) {
                enabledEvaluators.add(DatabaseConnectionPoolEvaluator.class );
            }
            if ( deadlockEvaluatorEnabled ) {
                enabledEvaluators.add( DeadlockEvaluator.class );
            }
            if ( taskEngineEvaluatorEnabled ) {
                enabledEvaluators.add( TaskEngineEvaluator.class );
            }

            // Change property values based on the parsed values.
            plugin.setTaskEnabled( false ); // disable momentarily, to allow settings to be applied without a surplus of restarts.
            plugin.setTaskEvaluatorClasses( enabledEvaluators );
            plugin.setTaskDelay( Duration.of( taskDelayMS, ChronoUnit.MILLIS ));
            plugin.setTaskInterval( Duration.of( taskIntervalMS, ChronoUnit.MILLIS ));
            plugin.setTaskBackoff( Duration.of( taskBackoffMS, ChronoUnit.MILLIS ));
            JiveGlobals.setProperty( "threaddump.evaluator.threadpools.interval", String.valueOf( poolEvaluatorIntervalMS ) );
            JiveGlobals.setProperty( "threaddump.evaluator.threadpools.busy-percentage-max", String.valueOf( poolEvaluatorBusyPercentage ) );
            JiveGlobals.setProperty( "threaddump.evaluator.threadpools.successive-hits", String.valueOf( poolEvaluatorSuccessiveHits ) );
            JiveGlobals.setProperty( "threaddump.evaluator.dbpool.interval", String.valueOf( dbPoolEvaluatorIntervalMS ) );
            JiveGlobals.setProperty( "threaddump.evaluator.dbpool.busy-percentage-max", String.valueOf( dbPoolEvaluatorBusyPercentage ) );
            JiveGlobals.setProperty( "threaddump.evaluator.dbpool.successive-hits", String.valueOf( dbPoolEvaluatorSuccessiveHits ) );
            JiveGlobals.setProperty( "threaddump.evaluator.deadlock.interval", String.valueOf( deadlockEvaluatorIntervalMS ));
            JiveGlobals.setProperty( "threaddump.evaluator.taskengine.max-threads", String.valueOf( taskEngineEvaluatorMaxThreads ) );
            JiveGlobals.setProperty( "threaddump.evaluator.taskengine.max-poolsize", String.valueOf( taskEngineEvaluatorMaxPoolSize ) );
            JiveGlobals.setProperty( "threaddump.evaluator.taskengine.interval", String.valueOf( taskEngineEvaluatorIntervalMS ) );
            JiveGlobals.setProperty( "threaddump.evaluator.taskengine.successive-hits", String.valueOf( taskEngineEvaluatorSuccessiveHits ) );
            plugin.setTaskEnabled( enableTask );

            webManager.logEvent( "Thread-dumping settings have been updated.", "task enabled: " + enableTask + "\nenabled evaluator classes: " + enabledEvaluators + "\ntask delay: " + taskDelayMS + "ms\ntask interval: " + taskIntervalMS + "ms\ntask backoff: " + taskBackoffMS + "ms\nthreadpool interval: " + poolEvaluatorIntervalMS + "ms\nthreadpool busy percentage: " + poolEvaluatorBusyPercentage + "%\nthreadpool successive hits: " + poolEvaluatorSuccessiveHits + "\ndatabase connection pool interval: " + dbPoolEvaluatorIntervalMS + "ms\ndatabase connection pool busy percentage: " + dbPoolEvaluatorBusyPercentage + "%\ndatabase connection pool successive hits: " + dbPoolEvaluatorSuccessiveHits + "\ndeadlock interval: " + deadlockEvaluatorIntervalMS + "ms\ntaskengine max threads: " + taskEngineEvaluatorMaxThreads + "\ntaskengine max pool size: " + taskEngineEvaluatorMaxPoolSize + "\ntaskengine successive hits: " + taskEngineEvaluatorSuccessiveHits + "\ntaskengine interval: " + taskEngineEvaluatorIntervalMS + "ms");
            response.sendRedirect( "threaddump.jsp?success=true" );
            return;
        }
    }
    csrfParam = StringUtils.randomString( 15 );
    CookieUtils.setCookie(request, response, "csrf", csrfParam, -1);
    pageContext.setAttribute( "csrf", csrfParam) ;

    final ThreadDump dump =  ThreadDump.getInstance();
    pageContext.setAttribute( "isDeadlocked", dump.getDeadLockedThreadIDs() != null && dump.getDeadLockedThreadIDs().length > 0 );
    pageContext.setAttribute( "dump",  new DefaultThreadDumpFormatter().format( dump ) );

    pageContext.setAttribute( "plugin", plugin );
//    pageContext.setAttribute( "isPoolEvaluatorEnabled", plugin.getTaskEvaluatorClasses().contains( CoreThreadPoolsEvaluator.class ) );
//    pageContext.setAttribute( "isPoolEvaluatorSupported", new CoreThreadPoolsEvaluator().isSupported() );
    pageContext.setAttribute( "isPoolEvaluatorEnabled", false ); // FIXME re-enable when issue #12 is fixed.
    pageContext.setAttribute( "isPoolEvaluatorSupported", false ); // FIXME re-enable when issue #12 is fixed.

    pageContext.setAttribute( "isDBPoolEvaluatorEnabled", plugin.getTaskEvaluatorClasses().contains( DatabaseConnectionPoolEvaluator.class ) );
    pageContext.setAttribute( "isDBPoolEvaluatorSupported", new DatabaseConnectionPoolEvaluator().isSupported() );
    pageContext.setAttribute( "isDeadlockEvaluatorEnabled", plugin.getTaskEvaluatorClasses().contains( DeadlockEvaluator.class ) );
    pageContext.setAttribute( "isDeadlockEvaluatorSupported", new DeadlockEvaluator().isSupported() );
    pageContext.setAttribute( "isTaskEngineEvaluatorEnabled", plugin.getTaskEvaluatorClasses().contains( TaskEngineEvaluator.class ) );
    pageContext.setAttribute( "isTaskEngineEvaluatorSupported", new TaskEngineEvaluator().isSupported() );

    pageContext.setAttribute( "poolEvaluatorInterval", JiveGlobals.getLongProperty( "threaddump.evaluator.threadpools.interval", Duration.of( 5, ChronoUnit.SECONDS ).toMillis() ) );
    pageContext.setAttribute( "poolEvaluatorSuccessiveHits", JiveGlobals.getIntProperty( "threaddump.evaluator.threadpools.successive-hits", 2 ) );
    pageContext.setAttribute( "poolEvaluatorBusyPercentage", JiveGlobals.getIntProperty( "threaddump.evaluator.threadpools.busy-percentage-max", 90 ) );

    pageContext.setAttribute( "dbPoolEvaluatorInterval", JiveGlobals.getLongProperty( "threaddump.evaluator.dbpool.interval", Duration.of( 5, ChronoUnit.SECONDS ).toMillis() ) );
    pageContext.setAttribute( "dbPoolEvaluatorSuccessiveHits", JiveGlobals.getIntProperty( "threaddump.evaluator.dbpool.successive-hits", 2 ) );
    pageContext.setAttribute( "dbPoolEvaluatorBusyPercentage", JiveGlobals.getIntProperty( "threaddump.evaluator.dbpool.busy-percentage-max", 90 ) );

    pageContext.setAttribute( "deadlockEvaluatorInterval", JiveGlobals.getLongProperty( "threaddump.evaluator.deadlock.interval", Duration.of( 5, ChronoUnit.MINUTES ).toMillis() ) );

    pageContext.setAttribute( "taskEngineEvaluatorMaxThreads", JiveGlobals.getIntProperty( "threaddump.evaluator.taskengine.max-threads", 300 ) );
    pageContext.setAttribute( "taskEngineEvaluatorMaxPoolSize", JiveGlobals.getIntProperty( "threaddump.evaluator.taskengine.max-poolsize", 500 ) );
    pageContext.setAttribute( "taskEngineEvaluatorInterval", JiveGlobals.getLongProperty( "threaddump.evaluator.taskengine.interval", Duration.of( 5, ChronoUnit.SECONDS ).toMillis() ) );
    pageContext.setAttribute( "taskEngineEvaluatorSuccessiveHits", JiveGlobals.getIntProperty( "threaddump.evaluator.taskengine.successive-hits", 2 ) );

%>
<html>
<head>
    <title><fmt:message key="threadump.page.title"/></title>
    <meta name="pageID" content="threaddump"/>
    <style>
        .disabled {
            pointer-events: none;
            background: lightgrey;
        }
    </style>
</head>
<body>

<% if (error != null) { %>

<div class="jive-error">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
        <tr><td class="jive-icon"><img src="/images/error-16x16.gif" width="16" height="16" border="0" alt=""></td>
            <td class="jive-icon-label">
                <% if ( "csrf".equalsIgnoreCase( error )  ) { %>
                <fmt:message key="global.csrf.failed" />
                <% } else { %>
                <fmt:message key="admin.error" />: <c:out value="error"></c:out>
                <% } %>
            </td></tr>
        </tbody>
    </table>
</div><br>

<%  } %>


<%  if (success != null) { %>

<div class="jive-info">
    <table cellpadding="0" cellspacing="0" border="0">
        <tbody>
        <tr><td class="jive-icon"><img src="/images/info-16x16.gif" width="16" height="16" border="0" alt=""></td>
            <td class="jive-info-text">
                <fmt:message key="settings.saved.successfully" />
            </td></tr>
        </tbody>
    </table>
</div><br>

<%  } %>

<p>
    <fmt:message key="threaddump.page.description"/>
</p>

<br>

<div class="jive-contentBoxHeader"><fmt:message key="threaddump.page.task.header" /></div>
<div class="jive-contentBox">

    <p><fmt:message key="threaddump.page.task.description" /></p>

    <p>
        <c:choose>
            <c:when test="${not empty plugin.lastDumpInstant}">
                <fmt:message key="threaddump.page.task.last-dump">
                    <fmt:param value="${plugin.lastDumpInstant}"/>
                </fmt:message>
            </c:when>
            <c:otherwise>
                <fmt:message key="threaddump.page.task.never-dumped"/>
            </c:otherwise>
        </c:choose>
    </p>

    <form name="taskConfig">
        <input type="hidden" name="csrf" value="${csrf}">

        <table width="80%" cellpadding="3" cellspacing="0" border="0">
            <tr>
                <td width="1%">
                    <input type="radio" name="enableTask" value="false" id="rb201" ${plugin.taskEnabled ? "" : "checked"} onClick="toggleReadOnly();">
                </td>
                <td width="99%">
                    <label for="rb201"><fmt:message key="threaddump.page.task.disabled" /></label>
                </td>
            </tr>
            <tr>
                <td width="1%" valign="top">
                    <input type="radio" name="enableTask" value="true" id="rb202" ${plugin.taskEnabled ? "checked" : ""} onClick="toggleReadOnly();"">
                </td>
                <td width="99%">
                    <label for="rb202"><fmt:message key="threaddump.page.task.enabled" /></label>

                    <div id="jive-roster" style="width: unset">
                        <p><b><fmt:message key="threaddump.page.task.settings.header"/></b></p>
                        <p><label for="taskDelayMS"><fmt:message key="threaddump.page.task.delay" /></label><br/>
                            <input type="number" min="0" id="taskDelayMS" name="taskDelayMS" size="45" maxlength="100" value="${plugin.taskDelay.toMillis()}"> <fmt:message key="global.milliseconds"/></p>
                        <p><label for="taskIntervalMS"><fmt:message key="threaddump.page.task.interval" /></label><br/>
                            <input type="number" min="0" id="taskIntervalMS" name="taskIntervalMS" size="45" maxlength="100" value="${plugin.taskInterval.toMillis()}"> <fmt:message key="global.milliseconds"/></p>
                        <p><label for="taskBackoffMS"><fmt:message key="threaddump.page.task.backoff" /></label><br/>
                            <input type="number" min="0" id="taskBackoffMS" name="taskBackoffMS" size="45" maxlength="100" value="${plugin.taskBackoff.toMillis()}"> <fmt:message key="global.milliseconds"/></p>
                    </div>
                    <br/>
                    <div id="jive-roster" style="width: unset">
                        <p><b><fmt:message key="threaddump.page.task.pools.title"/></b></p>
                        <c:if test="${not isPoolEvaluatorSupported}">
                            <div class="jive-info">
                                <table cellpadding="0" cellspacing="0" border="0">
                                    <tbody>
                                    <tr><td class="jive-icon"><img src="/images/info-16x16.gif" width="16" height="16" border="0" alt=""></td>
                                        <td class="jive-info-text">
                                            <fmt:message key="threaddump.page.task.unsupported-evaluator"/>
                                        </td></tr>
                                    </tbody>
                                </table>
                            </div>
                        </c:if>
                        <p><input type="checkbox" name="poolEvaluatorEnabled" id="poolEvaluatorEnabled" ${isPoolEvaluatorSupported && isPoolEvaluatorEnabled ? "checked" : ""} ${isPoolEvaluatorSupported ? "onClick='toggleReadOnly();'" : "style='display: none;'"}>
                            <label for="poolEvaluatorEnabled"><fmt:message key="threaddump.page.task.pools.enabled"/></label>
                        </p>
                        <p><label for="poolEvaluatorBusyPercentage"><fmt:message key="threaddump.page.task.pools.busy-percentage-max" /></label><br/>
                            <input type="number" min="0" max="100" id="poolEvaluatorBusyPercentage" name="poolEvaluatorBusyPercentage" size="45" maxlength="30" value="${poolEvaluatorBusyPercentage}"> %
                        <p><label for="poolEvaluatorIntervalMS"><fmt:message key="threaddump.page.task.pools.interval" /></label><br/>
                            <input type="number" min="0" id="poolEvaluatorIntervalMS" name="poolEvaluatorIntervalMS" size="45" maxlength="30" value="${poolEvaluatorInterval}"> <fmt:message key="global.milliseconds"/></p>
                        <p><label for="poolEvaluatorSuccessiveHits"><fmt:message key="threaddump.page.task.pools.successive-hits" /></label><br/>
                            <input type="number" min="1" id="poolEvaluatorSuccessiveHits" name="poolEvaluatorSuccessiveHits" size="45" maxlength="10" value="${poolEvaluatorSuccessiveHits}"></p>
                    </div>
                    <br/>
                    <div id="jive-roster" style="width: unset">
                        <p><b><fmt:message key="threaddump.page.task.dbpool.title"/></b></p>
                        <c:if test="${not isDBPoolEvaluatorSupported}">
                            <div class="jive-info">
                                <table cellpadding="0" cellspacing="0" border="0">
                                    <tbody>
                                    <tr><td class="jive-icon"><img src="/images/info-16x16.gif" width="16" height="16" border="0" alt=""></td>
                                        <td class="jive-info-text">
                                            <fmt:message key="threaddump.page.task.unsupported-evaluator"/>
                                        </td></tr>
                                    </tbody>
                                </table>
                            </div>
                        </c:if>
                        <p><input type="checkbox" name="dbPoolEvaluatorEnabled" id="dbPoolEvaluatorEnabled" ${isDBPoolEvaluatorSupported && isDBPoolEvaluatorEnabled ? "checked" : ""} ${isDBPoolEvaluatorSupported ? "onClick='toggleReadOnly();'" : "style='display: none;'"}>
                            <label for="dbPoolEvaluatorEnabled"><fmt:message key="threaddump.page.task.dbpool.enabled"/></label>
                        </p>
                        <p><label for="dbPoolEvaluatorBusyPercentage"><fmt:message key="threaddump.page.task.dbpool.busy-percentage-max" /></label><br/>
                            <input type="number" min="0" max="100" id="dbPoolEvaluatorBusyPercentage" name="dbPoolEvaluatorBusyPercentage" size="45" maxlength="30" value="${dbPoolEvaluatorBusyPercentage}"> %
                        <p><label for="dbPoolEvaluatorIntervalMS"><fmt:message key="threaddump.page.task.dbpool.interval" /></label><br/>
                            <input type="number" min="0" id="dbPoolEvaluatorIntervalMS" name="dbPoolEvaluatorIntervalMS" size="45" maxlength="30" value="${dbPoolEvaluatorInterval}"> <fmt:message key="global.milliseconds"/></p>
                        <p><label for="dbPoolEvaluatorSuccessiveHits"><fmt:message key="threaddump.page.task.dbpool.successive-hits" /></label><br/>
                            <input type="number" min="1" id="dbPoolEvaluatorSuccessiveHits" name="dbPoolEvaluatorSuccessiveHits" size="45" maxlength="10" value="${dbPoolEvaluatorSuccessiveHits}"></p>
                    </div>
                    <br/>
                    <div id="jive-roster" style="width: unset">
                        <p><b><fmt:message key="threaddump.page.task.deadlock.title"/></b></p>
                        <c:if test="${not isDeadlockEvaluatorSupported}">
                            <div class="jive-info">
                                <table cellpadding="0" cellspacing="0" border="0">
                                    <tbody>
                                    <tr><td class="jive-icon"><img src="/images/info-16x16.gif" width="16" height="16" border="0" alt=""></td>
                                        <td class="jive-info-text">
                                            <fmt:message key="threaddump.page.task.unsupported-evaluator"/>
                                        </td></tr>
                                    </tbody>
                                </table>
                            </div>
                        </c:if>
                        <p><input type="checkbox" name="deadlockEvaluatorEnabled" id="deadlockEvaluatorEnabled" ${isDeadlockEvaluatorSupported && isDeadlockEvaluatorEnabled ? "checked" : ""} ${isDeadlockEvaluatorSupported ? "onClick='toggleReadOnly();'" : "style='display: none;'"}>
                            <label for="deadlockEvaluatorEnabled"><fmt:message key="threaddump.page.task.deadlock.enabled"/></label>
                        </p>
                        <p><label for="deadlockEvaluatorIntervalMS"><fmt:message key="threaddump.page.task.deadlock.interval" /></label><br/>
                            <input type="number" min="0" id="deadlockEvaluatorIntervalMS" name="deadlockEvaluatorIntervalMS" size="45" maxlength="30" value="${deadlockEvaluatorInterval}"> <fmt:message key="global.milliseconds"/></p>
                    </div>
                    <br/>
                    <div id="jive-roster" style="width: unset">
                        <p><b><fmt:message key="threaddump.page.task.taskengine.title"/></b></p>
                        <c:if test="${not isTaskEngineEvaluatorSupported}">
                            <div class="jive-info">
                                <table cellpadding="0" cellspacing="0" border="0">
                                    <tbody>
                                    <tr><td class="jive-icon"><img src="/images/info-16x16.gif" width="16" height="16" border="0" alt=""></td>
                                        <td class="jive-info-text">
                                            <fmt:message key="threaddump.page.task.unsupported-evaluator"/>
                                        </td></tr>
                                    </tbody>
                                </table>
                            </div>
                        </c:if>
                        <p><input type="checkbox" name="taskEngineEvaluatorEnabled" id="taskEngineEvaluatorEnabled" ${isTaskEngineEvaluatorSupported && isTaskEngineEvaluatorEnabled ? "checked" : ""} ${isTaskEngineEvaluatorSupported ? "onClick='toggleReadOnly();'" : "style='display: none;'"}>
                            <label for="taskEngineEvaluatorEnabled"><fmt:message key="threaddump.page.task.taskengine.enabled"/></label>
                        </p>
                        <p><label for="taskEngineEvaluatorMaxThreads"><fmt:message key="threaddump.page.task.taskengine.max-threads" /></label><br/>
                            <input type="number" min="0" id="taskEngineEvaluatorMaxThreads" name="taskEngineEvaluatorMaxThreads" size="45" maxlength="30" value="${taskEngineEvaluatorMaxThreads}"></p>
                        <p><label for="taskEngineEvaluatorMaxPoolSize"><fmt:message key="threaddump.page.task.taskengine.max-poolsize" /></label><br/>
                            <input type="number" min="0" id="taskEngineEvaluatorMaxPoolSize" name="taskEngineEvaluatorMaxPoolSize" size="45" maxlength="30" value="${taskEngineEvaluatorMaxPoolSize}"></p>
                        <p><label for="taskEngineEvaluatorIntervalMS"><fmt:message key="threaddump.page.task.taskengine.interval" /></label><br/>
                            <input type="number" min="0" id="taskEngineEvaluatorIntervalMS" name="taskEngineEvaluatorIntervalMS" size="45" maxlength="30" value="${taskEngineEvaluatorInterval}"> <fmt:message key="global.milliseconds"/></p>
                        <p><label for="taskEngineEvaluatorSuccessiveHits"><fmt:message key="threaddump.page.task.taskengine.successive-hits" /></label><br/>
                            <input type="number" min="1" id="taskEngineEvaluatorSuccessiveHits" name="taskEngineEvaluatorSuccessiveHits" size="45" maxlength="10" value="${taskEngineEvaluatorSuccessiveHits}"></p>
                    </div>
                </td>
            </tr>
            <tr>
                <td width="1%"></td>
                <td width="99%">
                    <input type="submit" name="update" value="<fmt:message key="global.save_settings" />">
                </td>
            </tr>
        </table>
    </form>

</div>

<div class="jive-contentBoxHeader"><fmt:message key="threaddump.page.threaddump.header" /></div>
<div class="jive-contentBox">

    <p><fmt:message key="threaddump.page.threaddump.description" /></p>

    <c:if test="${isDeadlocked}">
        <div class="jive-warning">
            <table cellpadding="0" cellspacing="0" border="0">
                <tbody>
                <tr><td class="jive-icon"><img src="/images/warning-16x16.gif" width="16" height="16" border="0" alt=""></td>
                    <td class="jive-error-text">
                        <fmt:message key="threaddump.page.deadlock.detected" />
                    </td></tr>
                </tbody>
            </table>
        </div><br>
    </c:if>

    <textarea id="threaddump-ta" style="width: 100%" rows="40"><c:out value="${dump}"/></textarea>

    <button onclick="copyToClipboard()"><fmt:message key="threaddump.page.copy-to-clipboard" /></button>

</div>

    <script>
        function copyToClipboard() {
            var data = document.getElementById( "threaddump-ta" );
            data.select();
            document.execCommand( "copy" );
        }

        function toggleReadOnly()
        {
            var disabled = document.getElementById('rb201').checked;
            var poolDisabled = !document.getElementById('poolEvaluatorEnabled').checked;
            var dbPoolDisabled = !document.getElementById('dbPoolEvaluatorEnabled').checked;
            var deadlockDisabled = !document.getElementById('deadlockEvaluatorEnabled').checked;
            var taskEngineDisabled = !document.getElementById('taskEngineEvaluatorEnabled').checked;

            adjustClassList( disabled, 'taskDelayMS' );
            adjustClassList( disabled, 'taskIntervalMS' );
            adjustClassList( disabled, 'taskBackoffMS' );
            adjustClassList( disabled, 'poolEvaluatorEnabled' );
            adjustClassList( disabled || poolDisabled, 'poolEvaluatorBusyPercentage' );
            adjustClassList( disabled || poolDisabled, 'poolEvaluatorIntervalMS' );
            adjustClassList( disabled || poolDisabled, 'poolEvaluatorSuccessiveHits' );
            adjustClassList( disabled, 'dbPoolEvaluatorEnabled' );
            adjustClassList( disabled || dbPoolDisabled, 'dbPoolEvaluatorBusyPercentage' );
            adjustClassList( disabled || dbPoolDisabled, 'dbPoolEvaluatorIntervalMS' );
            adjustClassList( disabled || dbPoolDisabled, 'dbPoolEvaluatorSuccessiveHits' );
            adjustClassList( disabled, 'deadlockEvaluatorEnabled' );
            adjustClassList( disabled || deadlockDisabled, 'deadlockEvaluatorIntervalMS' );
            adjustClassList( disabled, 'taskEngineEvaluatorEnabled' );
            adjustClassList( disabled || taskEngineDisabled, 'taskEngineEvaluatorMaxThreads' );
            adjustClassList( disabled || taskEngineDisabled, 'taskEngineEvaluatorMaxPoolSize' );
            adjustClassList( disabled || taskEngineDisabled, 'taskEngineEvaluatorIntervalMS' );
            adjustClassList( disabled || taskEngineDisabled, 'taskEngineEvaluatorSuccessiveHits' );
        }

        function adjustClassList( doAdd, elementId )
        {
            if (doAdd) {
                document.getElementById(elementId).classList.add('disabled');
            } else {
                document.getElementById( elementId ).classList.remove( 'disabled' );
            }
        }

        toggleReadOnly();
    </script>
</body>
</html>
