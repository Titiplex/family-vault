-- Users
create table if not exists users
(
    id    integer primary key autoincrement,
    name  TEXT not null unique,
    email TEXT
);

-- Lists (task list)
create table if not exists lists
(
    id       integer primary key autoincrement,
    user_id  integer not null,
    title    TEXT    not null,
    done     integer not null default 0,
    due_date TEXT,
    foreign key (user_id) references users (id)
);

-- Events (calendar)
create table if not exists events
(
    id         integer primary key autoincrement,
    user_id    integer not null,
    title      TEXT    not null,
    event_date TEXT    not null,
    location   TEXT,
    notes      TEXT,
    foreign key (user_id) references users (id)
);

-- Recipes
create table if not exists recipes
(
    id          integer primary key autoincrement,
    user_id     integer not null,
    title       TEXT    not null,
    ingredients TEXT,
    steps       TEXT,
    foreign key (user_id) references users (id)
);

-- Messages
create table if not exists messages
(
    id        integer primary key autoincrement,
    user_id   integer not null,
    recipient TEXT,
    subject   TEXT,
    content   TEXT,
    timestamp TEXT,
    foreign key (user_id) references users (id)
);

-- Gallery
create table if not exists photos
(
    id      integer primary key autoincrement,
    user_id integer not null,
    path    TEXT    not null,
    caption TEXT,
    foreign key (user_id) references users (id)
);

-- Contacts
create table if not exists contacts
(
    id      integer primary key autoincrement,
    user_id integer not null,
    name    TEXT    not null,
    phone   TEXT,
    email   TEXT,
    foreign key (user_id) references users (id)
);

-- Activity
create table if not exists activities
(
    id               integer primary key autoincrement,
    user_id          integer not null,
    date             TEXT    not null,
    description      TEXT,
    duration_minutes integer,
    foreign key (user_id) references users (id)
);

-- Premium: Documents
create table if not exists documents
(
    id      integer primary key autoincrement,
    user_id integer not null,
    path    TEXT    not null,
    title   TEXT,
    tags    TEXT,
    foreign key (user_id) references users (id)
);

-- Premium: Budget
create table if not exists budget
(
    id       integer primary key autoincrement,
    user_id  integer not null,
    date     TEXT    not null,
    category TEXT,
    amount   real    not null,
    note     TEXT,
    foreign key (user_id) references users (id)
);

-- Premium: Meals
create table if not exists meals
(
    id       integer primary key autoincrement,
    user_id  integer not null,
    date     TEXT    not null,
    meal     TEXT,
    calories integer,
    foreign key (user_id) references users (id)
);

-- Premium: Timetable
create table if not exists timetable
(
    id          integer primary key autoincrement,
    user_id     integer not null,
    day_of_week integer not null, -- 1=Mon .. 7=Sun
    start_time  TEXT    not null,
    end_time    TEXT    not null,
    label       TEXT,
    foreign key (user_id) references users (id)
);

-- Premium: Map places
create table if not exists places
(
    id        integer primary key autoincrement,
    user_id   integer not null,
    name      TEXT    not null,
    latitude  real,
    longitude real,
    note      TEXT,
    foreign key (user_id) references users (id)
);