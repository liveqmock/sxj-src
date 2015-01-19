package com.sxj.supervisor.enu.rfid.applymanager;


public enum M_PayStateEnum
{
    PAYMENT("已付款", 0), NOT_PAYMENT("未付款", 1);
    
    // 成员变量
    private Integer id;
    
    private String name;
    
    private M_PayStateEnum(String name, Integer id)
    {
        this.name = name;
        this.id = id;
    }
    
    // 普通方法
    public static String getNamed(Integer id)
    {
        for (M_PayStateEnum c : M_PayStateEnum.values())
        {
            if (c.getId() == id)
            {
                return c.name;
            }
        }
        return null;
    }
    
    public Integer getId()
    {
        return id;
    }
    
    public void setId(Integer id)
    {
        this.id = id;
    }
    
    public String getName()
    {
        return name;
    }
    
    public void setName(String name)
    {
        this.name = name;
    }
}