package io.ermdev.cshop.data.repository;

import io.ermdev.cshop.data.dto.TokenDto;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface TokenRepository {

    @Insert("CREATE TABLE IF NOT EXISTS tbl_token(id BIGINT NOT NULL AUTO_INCREMENT, _key VARCHAR(100), expiryDate " +
            "VARCHAR(45), PRIMARY KEY(id))")
    void createTable();

    @Select("SELECT * FROM tbl_token WHERE id=#{tokenId}")
    TokenDto findById(@Param("tokenId") Long tokenId);

    @Select("SELECT * FROM tbl_token WHERE key=#{key}")
    TokenDto findByKey(@Param("key") String key);

    @Select("SELECT * FROM tbl_token")
    List<TokenDto> findAll();

    @Insert("INSERT INTO tbl_token(id, _key, expiryDate) VALUES(#{id}, #{key}, #{expiryDate})")
    void add(TokenDto token);

    @Update("UPDATE FROM tbl_token SET _key=#{key}, expiryDate=#{expiryDate} WHERE id=#{id}")
    void update(TokenDto token);

    @Delete("DELETE FROM tbl_token WHERE id=#{id} OR _key=#{key}")
    void delete(TokenDto token);
}
