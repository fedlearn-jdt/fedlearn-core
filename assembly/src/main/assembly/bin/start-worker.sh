#!/usr/bin/env bash

#
# Usage: start.sh [-c configFile] [-d]
# -d 表示debug模式
cd `dirname $0`
BIN_DIR=`pwd`
cd ..
DEPLOY_DIR=$(pwd)
DEPLOY_DIR_REAL=`readlink -f ${DEPLOY_DIR}`
CONF_DIR=${DEPLOY_DIR_REAL}/conf

CONFIG_FILE=""
JAVA_DEBUG_OPTS=""
while getopts ":c:d" opt
do
    case ${opt} in
        a) echo "参数a的值$OPTARG";;
        d) JAVA_DEBUG_OPTS=" -Xdebug -Xnoagent -Djava.compiler=NONE -Xrunjdwp:transport=dt_socket,address=8000,server=y,suspend=n " ;;
        c) CONFIG_FILE=$OPTARG;;
        :) # 没有为需要参数的选项指定参数
           echo "This option -$OPTARG requires an argument."
           exit 1;;
        ?) # 发现了无效的选项
           echo "-$OPTARG is not an option"
           exit 2 ;;
    esac
done

echo ${CONFIG_FILE}
SERVER_NAME=`grep -v '^\s*#' ${CONFIG_FILE} | sed '/app.name/!d;s/.*=//' | tr -d '\r'`
SERVER_PORT=`grep -v '^\s*#' ${CONFIG_FILE} | sed '/app.port/!d;s/.*=//' | tr -d '\r'`
LOG_FILE=`grep -v '^\s*#' ${CONFIG_FILE} | sed '/log.settings/!d;s/.*=//' | tr -d '\r'`

APP=`sed '/"APP"/!d;s/.*value="\(.*\)".*/\1/' ${LOG_FILE}`
eval LOGS_DIR=`sed '/"LOG_HOME"/!d;s/.*value="\(.*\)".*/\1/' ${LOG_FILE}`

echo $SERVER_NAME
echo $SERVER_PORT
echo ${LOG_FILE}
#exit 2

if [[ -z "$APP" ]]; then
  echo "ERROR: Not Found APP defined in ${LOG_FILE}"
  exit 1
fi

if [[ -z "$LOGS_DIR" ]]; then
  echo "ERROR: Not Found LOG_HOME defined in ${LOG_FILE}"
  exit 1
fi

if [[ -z "$SERVER_NAME" ]]; then
    SERVER_NAME=$APP
fi

if test -n "${JAVA_HOME}"; then
  if test -z "${JAVA_EXE}"; then
    JAVA_EXE=${JAVA_HOME}/bin/java
  fi
fi

if test -z "${JAVA_EXE}"; then
  JAVA_EXE=java
fi

${JAVA_EXE} -version >/dev/null 2>&1
if [[ $? -ne 0 ]]; then
  echo "ERROR: Not Found java installed!"
  exit 1
fi

PIDS=`ps -ef | grep java | grep "$CONF_DIR" |awk '{print $2}'`
if [[ -n "$PIDS" ]]; then
    echo "ERROR: The $SERVER_NAME already started!"
    echo "PID: $PIDS"
    exit 1
fi

if [[ -n "$SERVER_PORT" ]]; then
    SERVER_PORT_COUNT=`netstat -tln | grep ${SERVER_PORT} | wc -l`
    if [[ ${SERVER_PORT_COUNT} -gt 0 ]]; then
        echo "ERROR: The $SERVER_NAME port $SERVER_PORT already used!"
        exit 1
    fi
fi

if [[ -z "$LOGS_DIR" ]]; then
    LOGS_DIR=${DEPLOY_DIR}/logs
fi
if [[ ! -d ${LOGS_DIR} ]]; then
    mkdir ${LOGS_DIR}
fi
if [[ ! -d ${LOGS_DIR} ]]; then
  echo "ERROR: Please check LOGS_DIR=$LOGS_DIR is ok?"
  exit 1
fi
STDOUT_FILE=${LOGS_DIR}/console.log

LIB_DIR=${DEPLOY_DIR}/lib
LIB_JARS=`ls ${LIB_DIR}|grep .jar|awk '{print "'${LIB_DIR}'/"$0}'|tr "\n" ":"`

JAVA_OPTS=" -Djava.awt.headless=true -Djava.net.preferIPv4Stack=true "


JAVA_JMX_OPTS=""
if [[ "$1" = "jmx" ]]; then
    JAVA_JMX_OPTS=" -Dcom.sun.management.jmxremote.port=1099 -Dcom.sun.management.jmxremote.ssl=false -Dcom.sun.management.jmxremote.authenticate=false "
fi
JAVA_MEM_OPTS=""
BITS=`${JAVA_EXE} -version 2>&1 | grep -i 64-bit`
if [[ -n "$BITS" ]]; then
    JAVA_MEM_OPTS=" -server -Xmx12g -Xms12g -Xmn2g  -Xss256k -XX:+DisableExplicitGC -XX:+CMSClassUnloadingEnabled -XX:+UseConcMarkSweepGC -XX:+CMSParallelRemarkEnabled  -XX:LargePageSizeInBytes=128m -XX:+UseFastAccessorMethods -XX:+UseCMSInitiatingOccupancyOnly -XX:CMSInitiatingOccupancyFraction=70 "
else
    JAVA_MEM_OPTS=" -server -Xms1g -Xmx2g -XX:SurvivorRatio=2 -XX:+UseParallelGC "
fi
#add agent opts
#JAVA_AGENT_OPTS=""
#AGENT_LIB=`ls $LIB_DIR | grep sgm-agent* | grep .jar`
#if [ -n "$AGENT_LIB" ]; then
#    JAVA_AGENT_OPTS=" -Xbootclasspath/a:$LIB_DIR/$AGENT_LIB -javaagent:$LIB_DIR/$AGENT_LIB "
#fi
## sgm agent

#if [ -n "$SGM_OPTS" ]; then
#    SGM_OPTS=$SGM_OPTS
#fi
#$SGM_OPTS -jar
echo "Starting the $SERVER_NAME ..."
MAIN_CLASS="com.jdt.fedlearn.worker.WorkerHttpApp"
APP_CONFIG=""
if [[ -n "${CONFIG_FILE}" ]];then
   APP_CONFIG=" -c "${CONFIG_FILE}
fi
nohup ${JAVA_EXE} $SGM_OPTS  $JAVA_OPTS $JAVA_MEM_OPTS $JAVA_DEBUG_OPTS $JAVA_JMX_OPTS -classpath $CONF_DIR:$LIB_JARS ${MAIN_CLASS} $APP_CONFIG > $STDOUT_FILE 2>&1 &

COUNT=0
while [[ ${COUNT} -lt 1 ]]; do
    sleep 1
    if [[ -n "$SERVER_PORT" ]]; then
        COUNT=`netstat -an | grep ${SERVER_PORT} | wc -l`
        echo "netstat check count[$COUNT]"
    else
        COUNT=`ps -ef | grep java | grep "$DEPLOY_DIR" | awk '{print $2}' | wc -l`
        echo "ps check count[$COUNT]"
    fi
    if [[ ${COUNT} -gt 0 ]]; then
        break
    fi
done

echo "OK!"
PIDS=`ps -ef | grep java | grep "$DEPLOY_DIR" | awk '{print $2}'`
echo "PID: $PIDS"
echo "STDOUT: $STDOUT_FILE"

