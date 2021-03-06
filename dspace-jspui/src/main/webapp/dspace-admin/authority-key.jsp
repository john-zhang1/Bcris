<%--

    The contents of this file are subject to the license and copyright
    detailed in the LICENSE and NOTICE files at the root of the source
    tree and available online at

    http://www.dspace.org/license/

--%>
<%@ page language="java" pageEncoding="UTF-8" %>
<%@ page import="org.dspace.content.Item" %>
<%@ page import="java.util.List" %>
<%@ page import="java.util.Map" %>
<%@ page import="java.net.URLEncoder" %>
<%@page import="javax.servlet.jsp.jstl.fmt.LocaleSupport"%>

<%@ taglib uri="http://www.dspace.org/dspace-tags.tld" prefix="dspace" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%
	List<Item> items = (List<Item>) request.getAttribute("items");
    List<Item> items_uncertain = (List<Item>) request.getAttribute("items_uncertain");
	List<Item> items_ambiguos = (List<Item>) request.getAttribute("items_ambiguos");
	List<Item> items_novalue = (List<Item>) request.getAttribute("items_novalue");
	List<Item> items_failed = (List<Item>) request.getAttribute("items_failed");
	List<Item> items_notfound = (List<Item>) request.getAttribute("items_notfound");
	List<Item> items_unset = (List<Item>) request.getAttribute("items_unset");
	List<Item> items_reject = (List<Item>) request.getAttribute("items_reject");
	
    String authKey = (String) request.getAttribute("authKey");
    String label = (String) request.getAttribute("label");
    List<String> variants = (List<String>) request.getAttribute("variants");    
    String message = (String) request.getSession().getAttribute("authority.message");
    String nextKey = (String) request.getAttribute("next");
    String prevKey = (String) request.getAttribute("previous");
    Boolean required = (Boolean) request.getAttribute("required");
    boolean bRequired = required != null?required.booleanValue():false;
    
    String authority = request.getParameter("authority");
    String issued = request.getParameter("issued");
    
    String scopeParam;
	if (authority != null)
	{
	    scopeParam = "authority="+authority;
	}
	else
	{
	    scopeParam = "issued="+issued;
	}
%>


<dspace:layout locbar="link" navbar="admin" titlekey="jsp.dspace-admin.authority-key.title">
<table width="95%">
    <tr>      
      <td align="left"><h1><fmt:message key="jsp.dspace-admin.authority-key.heading">
        <fmt:param value="<%= authKey %>" />
    	</fmt:message></h1>   
      </td>
      <td align="right" class="standard">
      	<a target="_blank"
				href='<%=LocaleSupport.getLocalizedMessage(pageContext,
                                "help.site-admin.authority-key")%>'><fmt:message
				key="jsp.help" /></a>        
      </td>
    </tr>
</table>

<% if (message != null){ %>
    <div id="authority-message"><%= message %></div>
<%
    }
    request.getSession().removeAttribute("authority.message");
%>

<h3><fmt:message key="jsp.dspace-admin.authority-key.info-heading" /></h3>
<table>
<tr>
<td>
<fmt:message key="jsp.dspace-admin.authority-key.label" />: <%= label %><br/>
<fmt:message key="jsp.dspace-admin.authority-key.key" />: <%= authKey %><br/>
<%
    if (variants != null)
    {
        %>
<fmt:message key="jsp.dspace-admin.authority-key.variants" />:
        <%
            for (String var : variants)
            {
                if(var!=null) {
        %>
                <%= var %>;&nbsp;
        <%  	}
            }
   }
%><br />
</td>
</tr>
</table>
<h3><fmt:message key="jsp.dspace-admin.authority-key.items-heading" /></h3>

<form method="post">
    <input type="hidden" name="issued" value="<%= request.getParameter("issued") %>" />
    <input type="hidden" name="key" value="<%= authKey %>" />
    <% if(!items_ambiguos.isEmpty() || !items_uncertain.isEmpty() || !items_notfound.isEmpty() || !items_failed.isEmpty() || !items_unset.isEmpty() || !items_reject.isEmpty() || !items_novalue.isEmpty() ) { %>
    <% if(!items_ambiguos.isEmpty()) { %>
    <h3>	<fmt:message key="jsp.dspace-admin.authority-key.items-ambiguos">				
				<fmt:param><img src="../image/confidence/4-question.gif" title="Ambiguos" alt=""/></fmt:param>
			</fmt:message></h3>
    <dspace:itemlist items="<%= items_ambiguos %>" disableCrossLinks="true"
                     linkToEdit="true"  radioButton="false" inputName="items_ambiguos" />
    
                   <input type="submit" name="submit_accept" value="<fmt:message key="jsp.dspace-admin.authority-key.accept" />" class="btn btn-success"/>
<%
    if (!bRequired)
    {
 %>
    <input type="submit" name="submit_reject" value="<fmt:message key="jsp.dspace-admin.authority-key.reject" />"  class="btn btn-danger" />
<%
    } 
    }
     if(!items_uncertain.isEmpty()) { %>
    <h3><fmt:message key="jsp.dspace-admin.authority-key.items-uncertain" >				
				<fmt:param><img src="../image/confidence/5-pinion.gif" title="Uncertain" alt=""/></fmt:param>
			</fmt:message></h3>
    <dspace:itemlist items="<%= items_uncertain %>" disableCrossLinks="true"
                     linkToEdit="true"  radioButton="false" inputName="items_uncertain" />
    
    <input type="submit" name="submit_accept" value="<fmt:message key="jsp.dspace-admin.authority-key.accept" />" class="btn btn-success" />
<%
    if (!bRequired)
    {
 %>
    <input type="submit" name="submit_reject" value="<fmt:message key="jsp.dspace-admin.authority-key.reject" />"  class="btn btn-danger"/>
<%
    }
    }
     if(!items_novalue.isEmpty()) { %>
    <h3><fmt:message key="jsp.dspace-admin.authority-key.items-novalue" >				
				<fmt:param><img src="../image/authority/bug.png" title="No Value" alt=""/></fmt:param>
			</fmt:message></h3>
    <dspace:itemlist items="<%= items_novalue %>" disableCrossLinks="true"
                     linkToEdit="true"  radioButton="false" inputName="items_novalue" />
    
    <input type="submit" name="submit_accept" value="<fmt:message key="jsp.dspace-admin.authority-key.accept" />" class="btn btn-success" />
<%
    if (!bRequired)
    {
 %>
    <input type="submit" name="submit_reject" value="<fmt:message key="jsp.dspace-admin.authority-key.reject" />"  class="btn btn-danger" />
<%
    }
    }
     if(!items_failed.isEmpty()) { %>
    <h3><fmt:message key="jsp.dspace-admin.authority-key.items-failed">				
				<fmt:param><img src="../image/confidence/2-errortriangle.gif" title="Failed" alt=""/></fmt:param>
			</fmt:message></h3>
    <dspace:itemlist items="<%= items_failed %>" disableCrossLinks="true"
                     linkToEdit="true"  radioButton="false" inputName="items_failed" />
    
    <input type="submit" name="submit_accept" value="<fmt:message key="jsp.dspace-admin.authority-key.accept" />" class="btn btn-success" />
<%
    if (!bRequired)
    {
 %>
    <input type="submit" name="submit_reject" value="<fmt:message key="jsp.dspace-admin.authority-key.reject" />"  class="btn btn-danger" />
<%
    }
    }
     
     if(!items_notfound.isEmpty()) { %>
     <h3><fmt:message key="jsp.dspace-admin.authority-key.items-notfound">				
 				<fmt:param><img src="../image/confidence/2-errortriangle.gif" title="Failed" alt=""/></fmt:param>
 			</fmt:message></h3>
     <dspace:itemlist items="<%= items_notfound %>" disableCrossLinks="true"
                      linkToEdit="true"  radioButton="false" inputName="items_notfound" />
     
     <input type="submit" name="submit_accept" value="<fmt:message key="jsp.dspace-admin.authority-key.accept" />" class="btn btn-success" />
 <%
     if (!bRequired)
     {
  %>
     <input type="submit" name="submit_reject" value="<fmt:message key="jsp.dspace-admin.authority-key.reject" />"  class="btn btn-danger"/>
 <%
     }
     }
     if(!items_unset.isEmpty()) { %>
    <h3><fmt:message key="jsp.dspace-admin.authority-key.items-unset" >				
				<fmt:param><img src="../image/confidence/0-unauthored.gif" title="Unset" alt=""/></fmt:param>
			</fmt:message></h3>
    <dspace:itemlist items="<%= items_unset %>" disableCrossLinks="true"
                     linkToEdit="true"  radioButton="false" inputName="items_unset" />
    
    <input type="submit" name="submit_accept" value="<fmt:message key="jsp.dspace-admin.authority-key.accept" />" class="btn btn-success"/>
<%
    if (!bRequired)
    {
 %>
    <input type="submit" name="submit_reject" value="<fmt:message key="jsp.dspace-admin.authority-key.reject" />"  class="btn btn-danger"/>
<%
    }
    }
     if(!items_reject.isEmpty()) { %>
    <h3><fmt:message key="jsp.dspace-admin.authority-key.items-reject" >				
				<fmt:param><img src="../image/confidence/3-circleslash.gif" title="Reject" alt=""/></fmt:param>
			</fmt:message></h3>
    <dspace:itemlist items="<%= items_reject %>" disableCrossLinks="true"
                     linkToEdit="true"  radioButton="false" inputName="items_reject" />
    
                                              
    <input type="submit" name="submit_accept" value="<fmt:message key="jsp.dspace-admin.authority-key.accept" />" class="btn btn-success"/>
<%
    if (!bRequired)
    {
 %>
    <input type="submit" name="submit_reject" value="<fmt:message key="jsp.dspace-admin.authority-key.reject" />"  class="btn btn-danger"/>
<%
    }
    }
    }
    else {
%>
	<fmt:message key="jsp.dspace-admin.authority-key.message.none-items" />
<% } %>
</form>


<div class="authority-key-nav-link">
<% if (prevKey != null) { %>
<a class="btn btn-link" href="<%= request.getContextPath() %>/dspace-admin/authority?<%= scopeParam %>&key=<%= URLEncoder.encode(prevKey, "UTF-8") %>"><fmt:message key="jsp.dspace-admin.authority-key.previous" /></a>
<% } %>
<a class="btn btn-default" href="<%= request.getContextPath() %>/dspace-admin/authority?<%= scopeParam %>"><fmt:message key="jsp.dspace-admin.authority-key.list" /></a>
<% if (nextKey != null) { %>
<a class="btn btn-link" href="<%= request.getContextPath() %>/dspace-admin/authority?<%= scopeParam %>&key=<%= URLEncoder.encode(nextKey, "UTF-8") %>"><fmt:message key="jsp.dspace-admin.authority-key.next" /></a>
<% } %>
</div>
</dspace:layout>
