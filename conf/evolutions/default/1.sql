# --- !Ups

CREATE TABLE IF NOT EXISTS candidates
(
    candidate_id      VARCHAR(255) PRIMARY KEY,
    candidate_name    VARCHAR(255),
    party_affiliation VARCHAR(255),
    biography         TEXT,
    campaign_platform TEXT,
    photo_url         TEXT
);
CREATE TABLE IF NOT EXISTS voters
(
    voter_id            VARCHAR(255) PRIMARY KEY,
    voter_name          VARCHAR(255),
    date_of_birth       VARCHAR(255),
    gender              VARCHAR(255),
    nationality         VARCHAR(255),
    registration_number VARCHAR(255),
    address_street      VARCHAR(255),
    address_city        VARCHAR(255),
    address_state       VARCHAR(255),
    address_country     VARCHAR(255),
    address_postcode    VARCHAR(255),
    email               VARCHAR(255),
    phone_number        VARCHAR(255),
    cell_number         VARCHAR(255),
    picture             TEXT,
    registered_age      INTEGER
);

CREATE TABLE IF NOT EXISTS votes
(
    voter_id     VARCHAR(255),
    candidate_id VARCHAR(255),
    voting_time  VARCHAR(255),
    vote         int DEFAULT 1,
    PRIMARY KEY (voter_id)
);

# --- !Downs

DROP TABLE candidates;
DROP TABLE voters;
DROP TABLE votes;
