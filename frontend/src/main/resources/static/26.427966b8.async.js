(window.webpackJsonp=window.webpackJsonp||[]).push([[26],{g6js:function(t,a,e){"use strict";e.r(a),e.d(a,"default",function(){return W});e("IzEo");var n,r=e("bx4M"),s=(e("g9YV"),e("wCAj")),i=(e("+L6B"),e("2/Rp")),c=(e("5Dmo"),e("3S7+")),o=e("TbGu"),u=e.n(o),l=(e("miYZ"),e("tsqr")),d=e("/HRN"),p=e.n(d),h=e("WaGi"),m=e.n(h),f=e("ZDA2"),k=e.n(f),g=e("/+P4"),y=e.n(g),C=e("K47E"),E=e.n(C),v=e("N9n2"),x=e.n(v),M=e("xHqa"),N=e.n(M),I=(e("2qtc"),e("kLXV")),L=(e("5NDa"),e("5rEg")),q=e("q1tI"),w=e.n(q),R=e("MuoO"),D=e("NTd/"),S=e.n(D),T=e("7DNP"),W=(e("5CpO"),L.a.Search,I.a.confirm,Object(R.connect)(function(t){return{lists:t.taskLaunch.lists,username:t.user.currentUser.username,loading:t.loading.effects["taskLaunch/queryTaskList"]}})(n=function(t){function a(){var t,e;p()(this,a);for(var n=arguments.length,r=new Array(n),s=0;s<n;s++)r[s]=arguments[s];return e=k()(this,(t=y()(a)).call.apply(t,[this].concat(r))),N()(E()(e),"state",{dataCreateAll:[],dataCreate:[],keyWords:""}),N()(E()(e),"handleChange",function(t){e.setState({keyWords:t})}),N()(E()(e),"handleSearch",function(){e.queryTaskList(e.state.keyWords)}),N()(E()(e),"handleJumpDetail",function(t){(0,e.props.dispatch)(T.routerRedux.push("/task/launch/detail?id=".concat(t.taskId)))}),N()(E()(e),"queryTaskList",function(){var t=arguments.length>0&&void 0!==arguments[0]?arguments[0]:"",a=e.props,n=a.dispatch,r=a.username,s=l.a.loading(S.a.formatMessage({id:"total.searching"}),0);n({type:"taskLaunch/queryTaskList",payload:{username:r,category:"created"}}).then(function(a){e.setState({dataCreateAll:a&&u()(a)||[],dataCreate:a&&u()(a).filter(function(a){return-1!=a.taskName.indexOf(t)})}),s()}).catch(function(t){s()})}),N()(E()(e),"operateItem",function(t){var a=e.props.dispatch;return w.a.createElement("div",null,w.a.createElement("span",{className:"point",onClick:function(){a(T.routerRedux.push("/task/launch/add?type=edit&taskId=".concat(t.taskId)))}},"修改"),w.a.createElement("span",{className:"point",onClick:function(){a(T.routerRedux.push("/task/train/list"))}},"训练"))}),N()(E()(e),"launchColumns",function(){e.props.dispatch;return[{title:S.a.formatMessage({id:"total.taskId"}),dataIndex:"taskId"},{title:S.a.formatMessage({id:"total.taskName"}),dataIndex:"taskName",render:function(t,a,n){return w.a.createElement(c.a,{title:S.a.formatMessage({id:"total.detail"})},w.a.createElement("span",{className:"point",onClick:function(){return e.handleJumpDetail(a)}},t))}},{title:S.a.formatMessage({id:"total.participant"}),dataIndex:"participants",render:function(t,a,e){return w.a.createElement("span",null,t.join(","))}}]}),e}return x()(a,t),m()(a,[{key:"componentDidMount",value:function(){this.queryTaskList()}},{key:"componentWillUnmount",value:function(){(0,this.props.dispatch)({type:"taskLaunch/resetData"})}},{key:"render",value:function(){var t=this,a=this.props,e=(a.lists,a.dispatch),n=this.state,c=n.dataCreate,o=n.keyWords;return w.a.createElement(r.a,{title:S.a.formatMessage({id:"launch.initiated"})},w.a.createElement("div",null,w.a.createElement(i.a,{icon:"plus",type:"primary",style:{marginRight:"20px"},onClick:function(){e(T.routerRedux.push("/task/launch/add"))}},S.a.formatMessage({id:"launch.create"}))),w.a.createElement("div",{style:{padding:"20px 0"}},w.a.createElement(L.a,{placeholder:S.a.formatMessage({id:"total.taskName"}),style:{width:"20%",marginRight:"20px"},onChange:function(a){return t.handleChange(a.target.value)},value:o}),w.a.createElement(i.a,{icon:"search",type:"primary",style:{marginRight:"20px"},onClick:this.handleSearch},S.a.formatMessage({id:"total.search"}))),w.a.createElement(s.a,{columns:this.launchColumns(),dataSource:c||[]}))}}]),a}(q.PureComponent))||n)}}]);