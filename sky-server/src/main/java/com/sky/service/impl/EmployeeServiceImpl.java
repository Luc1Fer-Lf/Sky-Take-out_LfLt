package com.sky.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.sky.constant.MessageConstant;
import com.sky.constant.PasswordConstant;
import com.sky.constant.StatusConstant;
import com.sky.context.BaseContext;
import com.sky.dto.EmployeeDTO;
import com.sky.dto.EmployeeLoginDTO;
import com.sky.dto.EmployeePageQueryDTO;
import com.sky.entity.Employee;
import com.sky.exception.AccountLockedException;
import com.sky.exception.AccountNotFoundException;
import com.sky.exception.PasswordErrorException;
import com.sky.mapper.EmployeeMapper;
import com.sky.result.PageResult;
import com.sky.service.EmployeeService;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.DigestUtils;

import java.time.LocalDateTime;

@Service
public class EmployeeServiceImpl implements EmployeeService {

    @Autowired
    private EmployeeMapper employeeMapper;

    /**
     * 员工登录
     *
     * @param employeeLoginDTO
     * @return
     */
    public Employee login(EmployeeLoginDTO employeeLoginDTO) {
        String username = employeeLoginDTO.getUsername();
        String password = employeeLoginDTO.getPassword();

        //1、根据用户名查询数据库中的数据
        Employee employee = employeeMapper.getByUsername(username);

        //2、处理各种异常情况（用户名不存在、密码不对、账号被锁定）
        if (employee == null) {
            //账号不存在
            throw new AccountNotFoundException(MessageConstant.ACCOUNT_NOT_FOUND);
        }

        //密码比对
        // 进行md5加密，然后再进行比对
        password = DigestUtils.md5DigestAsHex(password.getBytes());

        if (!password.equals(employee.getPassword())) {
            //密码错误
            throw new PasswordErrorException(MessageConstant.PASSWORD_ERROR);
        }

        if (employee.getStatus() == StatusConstant.DISABLE) {
            //账号被锁定
            throw new AccountLockedException(MessageConstant.ACCOUNT_LOCKED);
        }

        //3、返回实体对象
        return employee;
    }

    /**
     * 新增员工
     * @param employeeDTO
     */
    @Override
    public void addEmp(EmployeeDTO employeeDTO) {
        //属性拷贝
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        //1、补充缺失的属性值
        employee.setPassword(DigestUtils.md5DigestAsHex(PasswordConstant.DEFAULT_PASSWORD.getBytes()));//设置默认密码并进行md5加密
        employee.setStatus(StatusConstant.ENABLE); // 设置账号状态为启用
        employee.setCreateTime(LocalDateTime.now());
        employee.setUpdateTime(LocalDateTime.now());
        //获取token并解析token拿到员工ID
        if(employee.getCreateUser() == null) {
            employee.setCreateUser(BaseContext.getCurrentId());//如果创建用户为空，则设置当前用户为创建用户
        }
        employee.setUpdateUser(BaseContext.getCurrentId());//更新当前操作用户
        //2、调用Mapper的新增方法，将员工的数据保存到数据库中
        employeeMapper.insert(employee);
    }

    /**
     * 员工分页查询
     * @param employeePageQueryDTO
     * @return
     */
    @Override
    public PageResult page(EmployeePageQueryDTO employeePageQueryDTO) {
        //1、设置分页参数
        PageHelper.startPage(employeePageQueryDTO.getPage(), employeePageQueryDTO.getPageSize());
        //2、调用分页查询方法，并强转为Page
        Page<Employee> page = employeeMapper.pageQuery(employeePageQueryDTO.getName());
        //3、封装分页结果并返回
        return new PageResult(page.getTotal(), page.getResult());
    }

    /**
     * 启用禁用员工账号
     * @param status
     * @param id
     */
    @Override
    public void startOrStop(Integer status, Long id) {
        Employee employee = Employee.builder()
                .status(status)
                .id(id)
                .updateTime(LocalDateTime.now())
                .updateUser(BaseContext.getCurrentId())
                .build();
        employeeMapper.update(employee); // 调用Mapper的更新方法，传入employee对象，更新员工账号状态
    }

    /**
     * 根据ID查询员工信息
     * @param id
     * @return
     */
    @Override
    public Employee getById(Long id) {
        return employeeMapper.getById(id);
    }

    /**
     * 编辑员工信息
     * @param employeeDTO
     */
    @Override
    public void updateEmp(EmployeeDTO employeeDTO) {
        //属性拷贝
        Employee employee = new Employee();
        BeanUtils.copyProperties(employeeDTO, employee);
        employee.setUpdateTime(LocalDateTime.now());
        employee.setUpdateUser(BaseContext.getCurrentId());
        employeeMapper.update(employee);
    }

}
