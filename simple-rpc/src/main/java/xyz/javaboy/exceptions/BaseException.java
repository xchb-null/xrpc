package xyz.javaboy.exceptions;

/**
 * @author XDD
 * @project rpc-demo
 * @date 2021/1/29
 * @description Good Good Study,Day Day Up.
 */
public class BaseException extends RuntimeException {

    private Integer code;

    private String msg;

    public BaseException(){
        super();
    }
    public BaseException(Integer code,String msg){
        super(msg);
        this.code = code;
        this.msg = msg;
    }
}
