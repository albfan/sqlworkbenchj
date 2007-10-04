/*
 * DependencyDeleterTest.java
 *
 * This file is part of SQL Workbench/J, http://www.sql-workbench.net
 *
 * Copyright 2002-2007, Thomas Kellerer
 * No part of this code maybe reused without the permission of the author.
 *
 * To contact the author please send an email to: support@sql-workbench.net
 */

package workbench.db;

import junit.framework.TestCase;
import workbench.TestUtil;

/**
 *
 * @author support@sql-workbench.net
 */
public class DependencyDeleterTest
	extends TestCase
{
	private WbConnection dbConnection;
	public DependencyDeleterTest(String testName)
	{
		super(testName);
	}

	public void testSimpleDelete()
	{
		try
		{
			createSimpleTables();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			fail(e.getMessage());
		}
	}

	private void createHRTables()
		throws Exception
	{
		String sql = "CREATE TABLE REGIONS \n" + 
             "( \n" + 
             "   REGION_ID   integer         NOT NULL, \n" + 
             "   REGION_NAME varchar(25)  NULL \n" + 
             "); \n" + 
             "ALTER TABLE REGIONS \n" + 
             "   ADD CONSTRAINT REG_ID_PK PRIMARY KEY (REGION_ID); \n" + 
             " \n" + 
             "CREATE TABLE DEPARTMENTS \n" + 
             "( \n" + 
             "   DEPARTMENT_ID   integer          NOT NULL, \n" + 
             "   DEPARTMENT_NAME varchar(30)  NOT NULL, \n" + 
             "   MANAGER_ID      integer          NULL, \n" + 
             "   LOCATION_ID     integer          NULL \n" + 
             "); \n" + 
             "ALTER TABLE DEPARTMENTS \n" + 
             "   ADD CONSTRAINT DEPT_ID_PK PRIMARY KEY (DEPARTMENT_ID); \n" + 
             " \n" + 
             "CREATE TABLE JOBS \n" + 
             "( \n" + 
             "   JOB_ID     varchar(10)  NOT NULL, \n" + 
             "   JOB_TITLE  varchar(35)  NOT NULL, \n" + 
             "   MIN_SALARY integer          NULL, \n" + 
             "   MAX_SALARY integer          NULL \n" + 
             "); \n" + 
             "ALTER TABLE JOBS \n" + 
             "   ADD CONSTRAINT JOB_ID_PK PRIMARY KEY (JOB_ID); \n" + 
             " \n" + 
             "CREATE TABLE EMPLOYEES \n" + 
             "( \n" + 
             "   EMPLOYEE_ID    integer          NOT NULL, \n" + 
             "   FIRST_NAME     varchar(20)  NULL, \n" + 
             "   LAST_NAME      varchar(25)  NOT NULL, \n" + 
             "   EMAIL          varchar(25)  NOT NULL, \n" + 
             "   PHONE_NUMBER   varchar(20)  NULL, \n" + 
             "   HIRE_DATE      timestamp               NOT NULL, \n" + 
             "   JOB_ID         varchar(10)  NOT NULL, \n" + 
             "   SALARY         real        NULL, \n" + 
             "   COMMISSION_PCT float        NULL, \n" + 
             "   MANAGER_ID     integer          NULL, \n" + 
             "   DEPARTMENT_ID  integer          NULL \n" + 
             "   ,CONSTRAINT EMP_SALARY_MIN CHECK (salary > 0) \n" + 
             "); \n" + 
             "ALTER TABLE EMPLOYEES \n" + 
             "   ADD CONSTRAINT EMP_EMP_ID_PK PRIMARY KEY (EMPLOYEE_ID); \n" + 
             " \n" + 
             "CREATE TABLE JOB_HISTORY \n" + 
             "( \n" + 
             "   EMPLOYEE_ID   integer          NOT NULL, \n" + 
             "   START_DATE    timestamp               NOT NULL, \n" + 
             "   END_DATE      timestamp               NOT NULL, \n" + 
             "   JOB_ID        varchar(10)  NOT NULL, \n" + 
             "   DEPARTMENT_ID integer          NULL \n" + 
             "   ,CONSTRAINT JHIST_DATE_INTERVAL CHECK (end_DATE > start_DATE) \n" + 
             "); \n" + 
             "ALTER TABLE JOB_HISTORY \n" + 
             "   ADD CONSTRAINT JHIST_EMP_ID_ST_DATE_PK PRIMARY KEY (EMPLOYEE_ID,START_DATE); \n" + 
             " \n" + 
             "CREATE TABLE LOCATIONS \n" + 
             "( \n" + 
             "   LOCATION_ID    integer          NOT NULL, \n" + 
             "   STREET_ADDRESS varchar(40)  NULL, \n" + 
             "   POSTAL_CODE    varchar(12)  NULL, \n" + 
             "   CITY           varchar(30)  NOT NULL, \n" + 
             "   STATE_PROVINCE varchar(25)  NULL, \n" + 
             "   COUNTRY_ID     CHAR(2)            NULL \n" + 
             "); \n" + 
             "ALTER TABLE LOCATIONS \n" + 
             "   ADD CONSTRAINT LOC_ID_PK PRIMARY KEY (LOCATION_ID); \n" + 
             " \n" + 
             "CREATE TABLE COUNTRIES \n" + 
             "( \n" + 
             "   COUNTRY_ID   CHAR(2)            NOT NULL, \n" + 
             "   COUNTRY_NAME varchar(40)  NULL, \n" + 
             "   REGION_ID    integer         NULL \n" + 
             "); \n" + 
             "ALTER TABLE COUNTRIES \n" + 
             "   ADD CONSTRAINT COUNTRY_C_ID_PK PRIMARY KEY (COUNTRY_ID); \n" + 
             " \n" + 
             "ALTER TABLE DEPARTMENTS \n" + 
             "  ADD CONSTRAINT DEPT_LOC_FK FOREIGN KEY (LOCATION_ID) \n" + 
             "  REFERENCES LOCATIONS (LOCATION_ID); \n" + 
             " \n" + 
             "ALTER TABLE DEPARTMENTS \n" + 
             "  ADD CONSTRAINT DEPT_MGR_FK FOREIGN KEY (MANAGER_ID) \n" + 
             "  REFERENCES EMPLOYEES (EMPLOYEE_ID); \n" + 
             " \n" + 
             "ALTER TABLE EMPLOYEES \n" + 
             "  ADD CONSTRAINT EMP_DEPT_FK FOREIGN KEY (DEPARTMENT_ID) \n" + 
             "  REFERENCES DEPARTMENTS (DEPARTMENT_ID); \n" + 
             " \n" + 
             "ALTER TABLE EMPLOYEES \n" + 
             "  ADD CONSTRAINT EMP_JOB_FK FOREIGN KEY (JOB_ID) \n" + 
             "  REFERENCES JOBS (JOB_ID); \n" + 
             " \n" + 
             "ALTER TABLE EMPLOYEES \n" + 
             "  ADD CONSTRAINT EMP_MANAGER_FK FOREIGN KEY (MANAGER_ID) \n" + 
             "  REFERENCES EMPLOYEES (EMPLOYEE_ID); \n" + 
             " \n" + 
             "ALTER TABLE JOB_HISTORY \n" + 
             "  ADD CONSTRAINT JHIST_JOB_FK FOREIGN KEY (JOB_ID) \n" + 
             "  REFERENCES JOBS (JOB_ID); \n" + 
             " \n" + 
             "ALTER TABLE JOB_HISTORY \n" + 
             "  ADD CONSTRAINT JHIST_EMP_FK FOREIGN KEY (EMPLOYEE_ID) \n" + 
             "  REFERENCES EMPLOYEES (EMPLOYEE_ID); \n" + 
             " \n" + 
             "ALTER TABLE JOB_HISTORY \n" + 
             "  ADD CONSTRAINT JHIST_DEPT_FK FOREIGN KEY (DEPARTMENT_ID) \n" + 
             "  REFERENCES DEPARTMENTS (DEPARTMENT_ID); \n" + 
             " \n" + 
             "ALTER TABLE LOCATIONS \n" + 
             "  ADD CONSTRAINT LOC_C_ID_FK FOREIGN KEY (COUNTRY_ID) \n" + 
             "  REFERENCES COUNTRIES (COUNTRY_ID);" + 
             " \n" + 
             "ALTER TABLE COUNTRIES \n" + 
             "  ADD CONSTRAINT COUNTR_REG_FK FOREIGN KEY (REGION_ID) \n" + 
             "  REFERENCES REGIONS (REGION_ID);";
		
		TestUtil util = new TestUtil("DependencyDeleter");
		this.dbConnection = util.getConnection();
		TestUtil.executeScript(dbConnection, sql);
	}
	
	private void createSimpleTables()
		throws Exception
	{
		String sql = "CREATE TABLE address \n" + 
					 "( \n" + 
					 "   id        integer  NOT NULL, \n" + 
					 "   person_id integer \n" + 
					 "); \n" + 
					 "ALTER TABLE address \n" + 
					 "   ADD CONSTRAINT address_pkey PRIMARY KEY (id); \n" + 
					 "CREATE TABLE person \n" + 
					 "( \n" + 
					 "   id        integer         NOT NULL, \n" + 
					 "   firstname varchar(50), \n" + 
					 "   lastname  varchar(50) \n" + 
					 "); \n" + 
					 "ALTER TABLE person \n" + 
					 "   ADD CONSTRAINT person_pkey PRIMARY KEY (id); \n" + 
					 " \n" + 
					 "ALTER TABLE address \n" + 
					 "  ADD CONSTRAINT fk_pers FOREIGN KEY (person_id) \n" + 
					 "  REFERENCES person (id); \n";
		
		TestUtil util = new TestUtil("DependencyDeleter");
		this.dbConnection = util.getConnection();
		TestUtil.executeScript(dbConnection, sql);
	}
}