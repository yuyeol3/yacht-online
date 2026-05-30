drop table if exists refresh_tokens cascade;
drop table if exists participated cascade;
drop table if exists game_rooms cascade;
drop table if exists played cascade;
drop table if exists games cascade;
drop table if exists users cascade;


create table if not exists users(
    id BIGINT auto_increment primary key,
    login_id varchar(50) not null unique,
    password varchar(255) not null,
    nickname varchar(50) not null unique,
    created_at TIMESTAMP not null,
    updated_at TIMESTAMP not null,
    role varchar(255) not null
);

create index users_login_id_index on users(login_id);
create index users_nickname_id_index on users(nickname);

create table if not exists refresh_tokens(
    id BINARY(32) primary key,
    valid_until datetime not null,
    user_id BIGINT not null,
    foreign key (user_id) references users(id) on delete cascade
);

create table if not exists games(
    id BIGINT auto_increment primary key,
    started_at TIMESTAMP not null,
    ended_at TIMESTAMP not null
);

create table if not exists played(
    id BIGINT auto_increment primary key,
    game_id BIGINT,
    user_id BIGINT,
    score INT not null,
    game_rank INT not null,
    game_result VARCHAR(255) not null,
    foreign key (game_id) references games(id) on delete cascade,
    foreign key (user_id) references users(id) on delete set null
);

create table if not exists game_rooms(
    id BIGINT auto_increment primary key,
    status varchar(255) not null,
    room_name varchar(255) not null,
    version BIGINT default 0,
    created_at TIMESTAMP not null,
    updated_at TIMESTAMP not null,
    host_id BIGINT,
    FOREIGN KEY (host_id) references users(id) on delete cascade
);

