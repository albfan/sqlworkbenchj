create table countries
(
   country_id    char(2)             not null,
   country_name  varchar(40),
   region_id     integer
   ,constraint country_c_id_pk primary key (country_id)
);

-- begin table departments
create table departments
(
   department_id    integer            not null,
   department_name  varchar(30)   not null,
   manager_id       integer,
   location_id      integer
);

alter table departments
   add constraint dept_id_pk
   primary key (department_id);

create table employees
(
   employee_id     integer       not null,
   first_name      varchar(20),
   last_name       varchar(25)   not null,
   email           varchar(25)   not null,
   phone_number    varchar(20),
   hire_date       date          not null,
   job_id          varchar(10)   not null,
   salary          decimal(8,2),
   commission_pct  decimal(2,2),
   manager_id      integer,
   department_id   integer
   ,constraint emp_salary_min check (salary > 0)
);

alter table employees
   add constraint emp_emp_id_pk
   primary key (employee_id);

create table jobs
(
   job_id      varchar(10)   not null,
   job_title   varchar(35)   not null,
   min_salary  integer,
   max_salary  integer
);

alter table jobs
   add constraint job_id_pk
   primary key (job_id);

create table job_history
(
   employee_id    integer       not null,
   start_date     date          not null,
   end_date       date          not null,
   job_id         varchar(10)   not null,
   department_id  integer
   ,constraint jhist_date_interval check (end_date > start_date)
);

alter table job_history
   add constraint jhist_emp_id_st_date_pk
   primary key (employee_id, start_date);

create table locations
(
   location_id     integer       not null,
   street_address  varchar(40),
   postal_code     varchar(12),
   city            varchar(30)   not null,
   state_province  varchar(25),
   country_id      char(2)
);

alter table locations
   add constraint loc_id_pk
   primary key (location_id);

create table regions
(
   region_id    integer              not null,
   region_name  varchar(25)
);

alter table regions
   add constraint reg_id_pk
   primary key (region_id);

alter table countries
  add constraint countr_reg_fk foreign key (region_id)
  references regions (region_id);

alter table departments
  add constraint dept_loc_fk foreign key (location_id)
  references locations (location_id);

alter table employees
  add constraint emp_dept_fk foreign key (department_id)
  references departments (department_id);

alter table employees
  add constraint emp_job_fk foreign key (job_id)
  references jobs (job_id);

alter table job_history
  add constraint jhist_dept_fk foreign key (department_id)
  references departments (department_id);

alter table job_history
  add constraint jhist_emp_fk foreign key (employee_id)
  references employees (employee_id);

alter table job_history
  add constraint jhist_job_fk foreign key (job_id)
  references jobs (job_id);

alter table locations
  add constraint loc_c_id_fk foreign key (country_id)
  references countries (country_id);


