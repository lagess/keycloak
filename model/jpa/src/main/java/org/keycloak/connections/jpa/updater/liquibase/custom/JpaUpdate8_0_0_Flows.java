package org.keycloak.connections.jpa.updater.liquibase.custom;

import liquibase.exception.CustomChangeException;
import liquibase.statement.core.InsertStatement;
import liquibase.statement.core.UpdateStatement;
import liquibase.structure.core.Table;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class JpaUpdate8_0_0_Flows extends CustomKeycloakTask {

    @Override
    protected void generateStatementsImpl() throws CustomChangeException {
        migrateBrowserFlow();



    }

    private void migrateBrowserFlow() throws CustomChangeException {

    }

    private void migrateBrowserFlow() throws CustomChangeException {
        // Retrieve all realms
        String realmTableName = database.correctObjectName("REALM", Table.class);

        List<String> realms = new ArrayList<>();
        try (PreparedStatement statement = jdbcConnection.prepareStatement("SELECT ID FROM " + realmTableName);
             ResultSet rs = statement.executeQuery()) {

            while (rs.next()) {
                String realm = rs.getString("ID");
                realms.add(realm);
            }

        } catch (Exception e) {
            throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
        }


        for (String realm : realms) {

            //--------------
            // AUTHENTICATION_FLOW
            //-----------------
            String authenticationFlowTableName = database.correctObjectName("AUTHENTICATION_FLOW", Table.class);


            String browserId = null;
            String formsId = null;
            String conditionalOtpId = UUID.randomUUID().toString();

            // Create Browser -Conditional Flow
            statements.add(
                    new InsertStatement(null, null, authenticationFlowTableName)
                            .addColumnValue("ID", conditionalOtpId)
                            .addColumnValue("ALIAS", "Browser - Conditional OTP")
                            .addColumnValue("DESCRIPTION", "Flow to determine if the OTP is required for the authentication")
                            .addColumnValue("REALM_ID", realm)
                            .addColumnValue("PROVIDER_ID", "basic-flow")
                            .addColumnValue("TOP_LEVEL", "FALSE")
                            .addColumnValue("BUILT_IN", "TRUE")
            );

            // Retrieve ID of browser, forms and newly created
            try (PreparedStatement statement = jdbcConnection.prepareStatement("SELECT ID, ALIAS FROM " + authenticationFlowTableName + " WHERE REALM_ID=?")) {
                statement.setString(1, realm);
                ResultSet rs = statement.executeQuery();

                while (rs.next()) {
                    String alias = rs.getString("ALIAS");

                    if ("forms".equals(alias)) {
                        formsId = rs.getString("ID");
                    } else if ("browser".equals(alias)) {
                        browserId = rs.getString("ID");
                    }
                }
            } catch (Exception e) {
                throw new CustomChangeException(getTaskId() + ": Exception when updating data from previous version", e);
            }


            //--------------
            // EXECUTION_FLOW
            //-----------------
            String authenticationExecutionTableName = database.correctObjectName("AUTHENTICATION_EXECUTION", Table.class);

            String uuid = UUID.randomUUID().toString();
            statements.add(
                    new InsertStatement(null, null, authenticationExecutionTableName)
                            .addColumnValue("ID", uuid)
                            .addColumnValue("REALM_ID", realm)
                            .addColumnValue("FLOW_ID", formsId)
                            .addColumnValue("REQUIREMENT", "1")
                            .addColumnValue("PRIORITY", "20")
                            .addColumnValue("AUTHENTICATOR_FLOW", "TRUE")
                            .addColumnValue("AUTH_FLOW_ID", conditionalOtpId)
            );

            //conditional-user-configured
            String uuidUserConfigured = UUID.randomUUID().toString();
            statements.add(
                    new InsertStatement(null, null, authenticationExecutionTableName)
                            .addColumnValue("ID", uuidUserConfigured)
                            .addColumnValue("AUTHENTICATOR", "conditional-user-configured")
                            .addColumnValue("REALM_ID", realm)
                            .addColumnValue("FLOW_ID", conditionalOtpId)
                            .addColumnValue("REQUIREMENT", "0")
                            .addColumnValue("PRIORITY", "10")
                            .addColumnValue("AUTHENTICATOR_FLOW", "FALSE")
            );

            //auth-otp-form
            statements.add(
                    new UpdateStatement(null, null, authenticationExecutionTableName)
                            .addNewColumnValue("FLOW_ID", conditionalOtpId)
                            .addNewColumnValue("REQUIREMENT", "0")
                            .addNewColumnValue("PRIORITY", "20")
                            .addNewColumnValue("AUTHENTICATOR_FLOW", "FALSE")
                            .setWhereClause("AUTHENTICATOR='auth-otp-form' AND FLOW_ID='"+formsId+"' AND REALM_ID='" + realm + "'")
            );

            confirmationMessage.append("Updated " + realm + " realm Browser flow");
        }
    }

    @Override
    protected String getTaskId() {
        return "Update 8.0.0";
    }
}
