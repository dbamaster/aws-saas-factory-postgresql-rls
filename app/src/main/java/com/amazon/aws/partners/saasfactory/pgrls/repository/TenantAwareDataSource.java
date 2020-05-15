/**
 * Copyright 2019 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this
 * software and associated documentation files (the "Software"), to deal in the Software
 * without restriction, including without limitation the rights to use, copy, modify,
 * merge, publish, distribute, sublicense, and/or sell copies of the Software, and to
 * permit persons to whom the Software is furnished to do so.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */
package com.amazon.aws.partners.saasfactory.pgrls.repository;

import com.amazon.aws.partners.saasfactory.pgrls.TenantContext;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Statement;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.datasource.lookup.AbstractRoutingDataSource;

/**
 * This is the single place where RLS policies in the database are tied
 * to the application. We are using a connection session variable to tell
 * PostgreSQL what the current tenant context is for that connection.
 * Connections are not shared and session variables are private so this is
 * thread safe. If we did not use a session variable, you'd have to create
 * a Postgres login ROLE for each tenant and then maintain a lookup
 * mechanism to get the proper connection credentials for each tenant.
 * @author mibeard
 */
public class TenantAwareDataSource extends AbstractRoutingDataSource {

	private static final Logger logger = LoggerFactory.getLogger(TenantAwareDataSource.class);
	
	@Override
	protected Object determineCurrentLookupKey() {
		return TenantContext.getTenant();
	}
	
	@Override
	public Connection getConnection() throws SQLException {
		// Every time the app asks the data source for a connection
		// set the PostgreSQL session variable to the current tenant
		// to enforce data isolation.
		Connection connection = super.getConnection();
		try (Statement sql = connection.createStatement()) {
			sql.execute("SET SESSION app.current_tenant = '" + determineCurrentLookupKey().toString() + "'");
		} catch (Exception e) {
			logger.error("Failed to execute: SET SESSION app.current_tenant = '" + determineCurrentLookupKey().toString() + "'");
			logger.error(e.getMessage());
		}
		return connection;
	}
}
