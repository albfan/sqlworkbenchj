create table countries
(
  country_id integer not null primary key
);

create table stores
(
   store_id     integer not null primary key,
   country_id   integer not null,
   region_id    integer not null,
   sales_mgr_id integer not null
);

create table store_details
(
  store_details_id     integer not null primary key,
  main_store           integer not null,
  alt_store            integer not null,
  vending_machine_id   integer not null
);
alter table store_details add constraint fk_sto_details_main_store foreign key (main_store) references stores (store_id);
alter table store_details add constraint fk_sto_details_alt_store foreign key (alt_store) references stores (store_id);

create table sto_details_item
(
  sdi_id           integer not null primary key,
  store_details_id integer not null
);
alter table sto_details_item add constraint fk_sdi_details foreign key (store_details_id) references store_details(store_details_id);


create table sto_details_data
(
  sdd_id integer not null primary key,
  store_details_id integer not null
);
alter table sto_details_data add constraint fk_sdd_details foreign key (store_details_id) references store_details(store_details_id);

create table regions
(
  region_id     integer not null primary key,
  country_id    integer not null
);
alter table regions add constraint fk_regions_country foreign key (country_id) references countries(country_id);

create table region_mgr
(
  region_mgr_id integer not null primary key,
  region_id     integer not null
);
alter table region_mgr add constraint fk_reg_mgr_region foreign key (region_id) references regions(region_id);

create table account_mgr
(
  acc_mgr_id    integer not null primary key,
  region_mgr_id integer not null
);
alter table account_mgr add constraint fk_acc_mgr_region foreign key (region_mgr_id) references region_mgr(region_mgr_id);

create table sales_mgr
(
  sales_mgr_id integer not null primary key,
  acc_mgr_id   integer not null
);
alter table sales_mgr add constraint fk_sales_mgr_region foreign key (acc_mgr_id) references account_mgr(acc_mgr_id);

create table vending_machines
(
   vending_machine_id integer not null primary key,
   region_id          integer not null
);
alter table vending_machines add constraint fk_vm_region foreign key (region_id) references regions(region_id);

alter table stores add constraint fk_store_country foreign key (country_id) references countries (country_id);
alter table stores add constraint fk_store_region foreign key (region_id) references regions (region_id);
alter table stores add constraint fk_store_sales_mgr foreign key (sales_mgr_id) references sales_mgr (sales_mgr_id);

alter table store_details add constraint fk_vm_sd foreign key (vending_machine_id) references vending_machines (vending_machine_id);

commit;