(window.webpackJsonp=window.webpackJsonp||[]).push([[25],{kLhj:function(t,e,n){"use strict";n.r(e),n.d(e,"default",function(){return P});n("IzEo");var a,r=n("bx4M"),i=(n("g9YV"),n("wCAj")),s=(n("+L6B"),n("2/Rp")),o=(n("Awhp"),n("KrTs")),c=(n("5Dmo"),n("3S7+")),u=(n("miYZ"),n("tsqr")),l=n("/HRN"),d=n.n(l),p=n("WaGi"),k=n.n(p),m=n("ZDA2"),f=n.n(m),h=n("/+P4"),y=n.n(h),T=n("K47E"),g=n.n(T),N=n("N9n2"),E=n.n(N),I=n("xHqa"),v=n.n(I),w=(n("y8nQ"),n("Vl3Y")),C=n("q1tI"),S=n.n(C),x=n("MuoO"),b=n("7DNP"),R=n("NTd/"),L=n.n(R),q=(w.a.Item,function(t){return L.a.formatMessage(t)}),P=Object(x.connect)(function(t){return{lists:t.taskTrain.lists,username:t.user.currentUser.username,loading:t.loading.effects["taskTrain/queryTrainList"]}})(a=function(t){function e(){var t,n;d()(this,e);for(var a=arguments.length,r=new Array(a),i=0;i<a;i++)r[i]=arguments[i];return n=f()(this,(t=y()(e)).call.apply(t,[this].concat(r))),v()(g()(n),"state",{visible:!1,record:{}}),v()(g()(n),"onCancel",function(){n.setState({visible:!1})}),v()(g()(n),"handleRefresh",function(){n.queryTrainList()}),v()(g()(n),"queryTrainList",function(){var t=n.props,e=t.dispatch,a=t.username,r=u.a.loading(q({id:"total.searching"}),0);e({type:"taskTrain/queryTrainList",payload:{username:a,taskId:null}}).then(function(){r()}).catch(function(){r()})}),v()(g()(n),"showdetail",function(t){var e=t.taskName,a=t.modelToken,r="COMPLETE"===t.runningStatus?2:1;(0,n.props.dispatch)(b.routerRedux.push("/task/train/detail?taskName=".concat(e,"&token=").concat(a,"&type=").concat(r)))}),v()(g()(n),"handleStop",function(t,e,a){var r=n.props;(0,r.dispatch)({type:e,payload:{username:r.username,modelToken:t.modelToken,type:a}}).then(function(t){0===t.code?(u.a.success(t.data.describes.join("，")),n.queryTrainList()):1===t.code&&u.a.error(t.data.describes.join("，"))})}),v()(g()(n),"operateItem",function(t){var e=n.props.dispatch,a=t.runningStatus;return S.a.createElement(C.Fragment,null,"COMPLETE"===a&&[S.a.createElement("span",{className:"point",onClick:function(){e(b.routerRedux.push("/task/predict/add?token=".concat(t.modelToken)))}},q({id:"total.reasoning"})),S.a.createElement("span",{className:"point",onClick:function(){e(b.routerRedux.push("/task/train/add?taskId=".concat(t.taskId,"&taskName=").concat(t.taskName,"&token=").concat(t.modelToken,"&type=edit")))}},q({id:"train.retrain"}))],"STOP"===a&&[S.a.createElement("span",{className:"point",onClick:function(){e(b.routerRedux.push("/task/train/add?taskId=".concat(t.taskId,"&taskName=").concat(t.taskName,"&token=").concat(t.modelToken,"&type=edit")))}},q({id:"train.retrain"}))],"RUNNING"===a&&[S.a.createElement("span",{className:"point",onClick:function(){return n.handleStop(t,"taskTrain/resolveTaskStop","stop")}},q({id:"train.stopBtn"})),S.a.createElement("span",{className:"point",onClick:function(){return n.handleStop(t,"taskTrain/resolveTaskSuspend","suspend")}},q({id:"train.waiting"}))],"SUSPEND"===a&&[S.a.createElement("span",{className:"point",onClick:function(){return n.handleStop(t,"taskTrain/resolveTaskStop","stop")}},q({id:"train.stopBtn"})),S.a.createElement("span",{className:"point",onClick:function(){return n.handleStop(t,"taskTrain/resolveTaskRestart","resume")}},q({id:"train.restart"}))])}),v()(g()(n),"trainColumns",function(){n.props.dispatch;return[{title:q({id:"total.taskId"}),key:"taskId",dataIndex:"taskId"},{title:q({id:"total.taskName"}),key:"taskName",dataIndex:"taskName"},{title:q({id:"total.modelId"}),key:"modelToken",dataIndex:"modelToken",render:function(t,e,a){return S.a.createElement(c.a,{title:q({id:"total.detail"})},S.a.createElement("span",{className:"point",onClick:function(){return n.showdetail(e)}},t))}},{title:q({id:"train.status"}),key:"runningStatus",dataIndex:"runningStatus",render:function(t,e,n){var a="",r="";switch(t.toLowerCase()){case"running":a="train.running",r="processing";break;case"suspend":a="train.waiting",r="warning";break;case"complete":a="train.complete",r="success";break;case"stop":a="train.stopBtn",r="warning";break;case"fail":a="train.fail",r="error";break;default:a="train.unKnown",r="default"}return S.a.createElement(o.a,{status:r,text:q({id:a})})}},{title:q({id:"total.operate"}),key:"operate",dataIndex:"operate",render:function(t,e,a){return n.operateItem(e)}}]}),n}return E()(e,t),k()(e,[{key:"componentDidMount",value:function(){this.queryTrainList()}},{key:"render",value:function(){var t=this.props,e=t.lists,n=t.dispatch;return S.a.createElement(r.a,{title:q({id:"train.list"})},S.a.createElement("div",null,S.a.createElement(s.a,{icon:"undo",type:"primary",style:{marginRight:"20px"},onClick:this.handleRefresh},q({id:"total.refresh"})),S.a.createElement(s.a,{icon:"plus",type:"primary",style:{marginRight:"20px"},onClick:function(){n(b.routerRedux.push("/task/train/add"))}},q({id:"train.create"}))),S.a.createElement("div",{style:{padding:"20px 0"}},S.a.createElement(i.a,{dataSource:e,columns:this.trainColumns(),rowKey:function(t,e){return e}})))}}]),e}(C.PureComponent))||a},t33a:function(t,e,n){t.exports=n("cHUP")(10)}}]);