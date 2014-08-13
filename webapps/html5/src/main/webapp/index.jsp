<%@ page import="be.xhibit.teletask.client.TeletaskClient" %>
<%@ page import="be.xhibit.teletask.model.spec.ClientConfigSpec" %>
<%@ page import="be.xhibit.teletask.model.spec.Function" %>
<%@ page import="be.xhibit.teletask.webapp.ClientHolder" %>
<%@ page contentType="text/html;charset=UTF-8" language="java" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<html>
<head>
    <title>Teletask UI</title>
    <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="apple-mobile-web-app-capable" content="yes">
    <meta name="apple-mobile-web-app-status-bar-style" content="black" />

    <link rel="stylesheet" href="css/jquery.mobile-1.4.3.min.css" />
    <link rel="stylesheet" href="css/style.css" />
    <link rel="apple-touch-icon" href="apple-touch-icon.png"/>

    <script src="js/jquery-1.11.1.min.js"></script>
    <script src="js/app-jsp.js"></script>
    <script src="js/jquery.mobile-1.4.3.min.js"></script>
</head>
<body>

    <%
        TeletaskClient client = ClientHolder.getClient();
        ClientConfigSpec tdsConfig = client.getConfig();

        request.setAttribute("tds_relays", tdsConfig.getComponents(Function.RELAY));
        request.setAttribute("tds_locmoods", tdsConfig.getComponents(Function.LOCMOOD));
        request.setAttribute("tds_genmoods", tdsConfig.getComponents(Function.GENMOOD));
        request.setAttribute("tds_motors", tdsConfig.getComponents(Function.MOTOR));
        request.setAttribute("tds_sensors", tdsConfig.getComponents(Function.SENSOR));
        request.setAttribute("tds_rooms", tdsConfig.getRooms());
    %>

    <div data-role="header" data-position="fixed" data-theme="b" data-add-back-btn="true">
        <h1>Teletask UI</h1>
        <a href="#page_start" class="ui-btn-left ui-btn ui-icon-home ui-btn-icon-notext ui-corner-all">Home</a>
    </div>

    <!-- ####################### page:start ####################### -->
    <div data-role="page" data-theme="a" id="page_start">

        <div data-role="content">
            <p><a href="#page_lights" data-role="button" data-icon="arrow-r" data-iconpos="bottom" class="ui-nodisc-icon ui-btn-icon-bottom icon-brightness-contrast">Lights</a></p>
            <p><a href="#page_moods" data-role="button" data-icon="arrow-r" data-iconpos="bottom" class="ui-nodisc-icon ui-btn-icon-bottom icon-equalizer">Moods</a></p>
            <p><a href="#page_screens" data-role="button" data-icon="arrow-r" data-iconpos="bottom" class="ui-nodisc-icon ui-btn-icon-bottom icon-menu">Screens</a></p>
            <p><a href="#page_sensors" data-role="button" data-icon="arrow-r" data-iconpos="bottom" class="ui-nodisc-icon ui-btn-icon-bottom icon-eye">Sensors</a></p>
        </div>

    </div>
    <!-- ####################### /page:start ####################### -->

    <!-- ####################### page:screens ####################### -->
    <div data-role="page" data-theme="a" id="page_screens" data-add-back-btn="true" data-title="Screens">

        <div data-role="content">
            <h3 class="ui-bar ui-bar-a ui-corner-all">Screens:</h3>

            <div class="ui-body ui-body-a ui-corner-all">
            <c:forEach items="${requestScope.tds_motors}" var="motor">
                <p>
                    <label for="MOTOR-SWITCH-<c:out value="${motor.number}" />"><c:out value="${motor.description}" />:</label>
                    <input type="checkbox" data-role="flipswitch" name="MOTOR-SWITCH-<c:out value="${motor.number}" />" class="stateSwitch" data-on-text="Down" data-off-text="Up" data-wrapper-class="custom-label-flipswitch" id="MOTOR-SWITCH-<c:out value="${motor.number}" />" data-tds-type="motor" data-tds-number="<c:out value="${motor.number}" />">
                </p>
            </c:forEach>
            </div>

        </div>

    </div>
    <!-- ####################### /page:screens ####################### -->

    <!-- ####################### page:moods ####################### -->
    <div data-role="page" data-theme="a" id="page_moods" data-add-back-btn="true" data-title="Moods">

        <div data-role="content">
            <h3 class="ui-bar ui-bar-a ui-corner-all">Local Moods:</h3>

            <div class="ui-body ui-body-a ui-corner-all">
            <c:forEach items="${requestScope.tds_locmoods}" var="locmood">
                <p>
                    <label for="LOCMOOD-SWITCH-<c:out value="${locmood.number}" />"><c:out value="${locmood.description}" />:</label>
                    <input type="checkbox" data-role="flipswitch" name="LOCMOOD-SWITCH-<c:out value="${locmood.number}" />" class="stateSwitch" id="LOCMOOD-SWITCH-<c:out value="${locmood.number}" />" data-tds-type="locmood" data-tds-number="<c:out value="${locmood.number}" />">
                </p>
            </c:forEach>
            </div>

            <h3 class="ui-bar ui-bar-a ui-corner-all">General Moods:</h3>

            <div class="ui-body ui-body-a ui-corner-all">
            <c:forEach items="${requestScope.tds_genmoods}" var="genmood">
                <p>
                    <label for="GENMOOD-SWITCH-<c:out value="${genmood.number}" />"><c:out value="${genmood.description}" />:</label>
                    <input type="checkbox" data-role="flipswitch" name="GENMOOD-SWITCH-<c:out value="${genmood.number}" />" class="stateSwitch" id="GENMOOD-SWITCH-<c:out value="${genmood.number}" />" data-tds-type="genmood" data-tds-number="<c:out value="${genmood.number}" />">
                </p>
            </c:forEach>
            </div>

        </div>

    </div>
    <!-- ####################### /page:moods ####################### -->

    <!-- ####################### page:verlichting ####################### -->
    <div data-role="page" data-theme="a" id="page_lights" data-add-back-btn="true" data-title="Lights">

        <div data-role="content">

            <div data-role="collapsibleset" data-collapsed-icon="carat-d" data-expanded-icon="carat-u">
            <c:forEach items="${requestScope.tds_rooms}" var="room">
                <div data-role="collapsible">
                    <h3><c:out value="${room.name}" /></h3>

                        <c:forEach items="${room.relays}" var="relay">
                        <p>
                            <label for="RELAY-SWITCH-<c:out value="${relay.number}" />"><c:out value="${relay.description}" /></label>
                            <input type="checkbox" data-role="flipswitch" name="RELAY-SWITCH-<c:out value="${relay.number}" />" class="stateSwitch" id="RELAY-SWITCH-<c:out value="${relay.number}" />" data-tds-type="relay" data-tds-number="<c:out value="${relay.number}" />">
                        </p>
                        </c:forEach>

                </div>
            </c:forEach>
            </div>
        </div>

    </div>
    <!-- ####################### /page:verlichting ####################### -->

    <!-- ####################### page:sensor ####################### -->
    <div data-role="page" data-theme="a" id="page_sensors" data-add-back-btn="true" data-title="Sensors">

        <div data-role="content">

            <div data-role="collapsibleset" data-collapsed-icon="carat-d" data-expanded-icon="carat-u">
                <c:forEach items="${requestScope.tds_sensors}" var="sensor">
                    <h3 class="ui-bar ui-bar-a ui-corner-all"><c:out value="${sensor.description}" /></h3>
                    <div class="ui-body">
                        <p id="SENSOR-<c:out value="${sensor.type}" />-<c:out value="${sensor.number}" />"><c:out value="${sensor.state}" />&nbsp; Lux</p>
                    </div>
                </c:forEach>
            </div>
        </div>

    </div>
    <!-- ####################### /page:sensor ####################### -->

    <div data-role="footer" data-position="fixed" data-theme="b">
        <div data-role="navbar">
            <ul>
                <li><a href="#page_lights" id="navbar_verlichting" class="ui-nodisc-icon ui-btn-icon-bottom icon-brightness-contrast"></a></li>
                <li><a href="#page_moods" id="navbar_sfeer" class="ui-nodisc-icon ui-btn-icon-bottom icon-equalizer"></a></li>
                <li><a href="#page_screens" id="navbar_screens" class="ui-nodisc-icon ui-btn-icon-bottom icon-menu"></a></li>
                <li><a href="#page_sensors" id="navbar_sensors" class="ui-nodisc-icon ui-btn-icon-bottom icon-eye"></a></li>
                <!--<li><a href="#page_rooms" id="navbar_allesuit" class="ui-nodisc-icon ui-btn-icon-bottom icon-power"></a></li>-->
            </ul>
        </div>
    </div>

</body>
</html>