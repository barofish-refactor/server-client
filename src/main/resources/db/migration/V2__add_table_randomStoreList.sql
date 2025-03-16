CREATE TABLE random_store_list (
   store_id INT NOT NULL PRIMARY KEY,
   CONSTRAINT fk_random_store_list_store FOREIGN KEY (store_id) REFERENCES store (id) ON DELETE CASCADE
);
