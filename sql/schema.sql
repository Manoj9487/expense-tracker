create database if not exists expense_tracker_db;
use expense_tracker_db;

create table expenses (
    id bigint auto_increment primary key,
    title varchar(100) not null,
    amount decimal(10, 2) not null,
    category varchar(50) not null,
    expense_date date not null,
    payment_method varchar(50) not null,
    description varchar(255) null,
    created_at timestamp not null default current_timestamp
);

create index idx_expenses_category on expenses(category);
create index idx_expenses_expense_date on expenses(expense_date);

create table users (
    id bigint auto_increment primary key,
    username varchar(50) not null unique,
    password varchar(255) not null,
    created_at timestamp not null default current_timestamp
);

alter table expenses add column user_id bigint not null;
alter table expenses add constraint fk_expenses_user foreign key (user_id) references users(id);