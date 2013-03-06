<%@ page contentType="text/html;charset=UTF-8" language="java" isELIgnored="false" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<html>
  <head>
    <title>Next Meeting</title>
    <link type="text/css" rel="stylesheet" href="AppsMarketplaceDemo.css">
  </head>
  <body>
    <div id="header">
      <p>AppsMarketplaceDemo</p>
    </div>
    <div id="event">
      <div class="messageTitle">
        Hello <c:out value="${user.firstName}"/><br/>
        Your Current or Next Calendar Events
      </div>
      <c:choose>
        <c:when test="${nextEvents != null}">
          <c:forEach var="nextEvent" items="${nextEvents}">
            <div class="eventTitle">
              <c:out value="${nextEvent.title.plainText}"/>
            </div>
              <b>When:</b> <c:out value="${nextEvent.times[0].startTime}"/> - <c:out value="${nextEvent.times[0].endTime}"/><br/>
              <b>Where:</b> <c:out value="${nextEvent.locations[0].valueString}"/><br/>
              <b>Description:</b> <c:out value="${nextEvent.plainTextContent}"/><br/><br/>
          </c:forEach>
        </c:when>
        <c:otherwise>
          <br/><b>You have no upcoming calendar events.</b><br/><br/>
        </c:otherwise>
      </c:choose>
    </div>
    <div id="footer">
      <p>More information can be found here: <a href="http://code.google.com/eclipse/docs/users_guide.html">Getting started with Apps Marketplace and Eclipse</a>.</p>
    </div>
  </body>
</html>