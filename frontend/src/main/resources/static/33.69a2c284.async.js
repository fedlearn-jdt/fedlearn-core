(window.webpackJsonp=window.webpackJsonp||[]).push([[33],{"O5/Y":function(e,a,n){"use strict";n.r(a),n.d(a,"default",function(){return T});n("IzEo");var t,r=n("bx4M"),s=(n("g9YV"),n("wCAj")),i=(n("+L6B"),n("2/Rp")),u=(n("miYZ"),n("tsqr")),o=n("/HRN"),c=n.n(o),d=n("WaGi"),l=n.n(d),m=n("ZDA2"),f=n.n(m),p=n("/+P4"),g=n.n(p),M=n("K47E"),h=n.n(M),y=n("N9n2"),I=n.n(y),v=n("xHqa"),x=n.n(v),E=n("q1tI"),N=n.n(E),w=n("MuoO"),b=n("7DNP"),k=n("NTd/"),q=n.n(k),C=(n("NMMd"),function(e){return q.a.formatMessage(e)}),T=Object(w.connect)(function(e){return{lists:e.userManager.lists,username:e.user.currentUser.username,loading:e.loading.effects["userManager/queryUserList"]}})(t=function(e){function a(){var e,n;c()(this,a);for(var t=arguments.length,r=new Array(t),s=0;s<t;s++)r[s]=arguments[s];return n=f()(this,(e=g()(a)).call.apply(e,[this].concat(r))),x()(h()(n),"state",{curTab:"join",dataJoinFilter:[],taskName:"",owner:"",visible:!1,record:{}}),x()(h()(n),"onCancel",function(){n.setState({visible:!1})}),x()(h()(n),"queryUserList",function(){var e=n.props,a=e.dispatch,t=e.username,r=u.a.loading(C({id:"total.searching"}),0);console.info(t),a({type:"userManager/queryUserList",payload:{username:t}}).then(function(){r()}).catch(function(){r()})}),x()(h()(n),"handleJumpEdit",function(e){(0,n.props.dispatch)(b.routerRedux.push("/user/manager/edit?userName=".concat(e.username,"&email=").concat(e.email,"&roles=").concat(e.roles,"&status=").concat(e.status)))}),x()(h()(n),"joinColumns",function(){return[{title:q.a.formatMessage({id:"userManager.userId"}),dataIndex:"userId"},{title:q.a.formatMessage({id:"userManager.userName"}),dataIndex:"username"},{title:q.a.formatMessage({id:"userManager.roles"}),dataIndex:"roles",render:function(e,a,n){return"SUPER_ADMIN"===e?"超级管理员":"ADMIN"===e?"管理员":"用户"}},{title:q.a.formatMessage({id:"userManager.status"}),dataIndex:"status",render:function(e,a,n){return"0"===e?"启用":"禁用"}},{title:q.a.formatMessage({id:"userManager.createTime"}),dataIndex:"createTime"},{title:q.a.formatMessage({id:"userManager.modifiedTime"}),dataIndex:"modifiedTime"},{title:q.a.formatMessage({id:"userManager.operate"}),render:function(e,a,t){return N.a.createElement("span",{className:"point",onClick:function(){return n.handleJumpEdit(a)}},"编辑")}}]}),n}return I()(a,e),l()(a,[{key:"componentDidMount",value:function(){this.queryUserList()}},{key:"render",value:function(){var e=this.props,a=e.lists,n=e.dispatch;return N.a.createElement(r.a,{title:q.a.formatMessage({id:"userManager.userManager"})},N.a.createElement("div",null,N.a.createElement(i.a,{icon:"plus",type:"primary",onClick:function(){n(b.routerRedux.push("/user/manager/add"))}},q.a.formatMessage({id:"userManager.add"}))),N.a.createElement("div",{style:{padding:"20px 0"}},N.a.createElement(s.a,{dataSource:a,columns:this.joinColumns(),rowKey:function(e,a){return a}})))}}]),a}(E.PureComponent))||t}}]);