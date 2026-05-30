// SeckillUtil.js - 秒杀系统通用工具类
const SeckillUtil = {
  baseURL: "http://127.0.0.1:8080/api",

  request: function (path, method = "GET", data = {}) {
    let url = this.baseURL + path;
    const options = {
      method: method,
      headers: {
        "Content-Type": "application/json",
        "Authorization": localStorage.getItem("admin_token") || "",
      },
    };

    if (method.toUpperCase() === "GET") {
      const params = new URLSearchParams(data);
      url += "?" + params.toString();
    } else {
      options.body = JSON.stringify(data);
    }

    return fetch(url, options)
      .then((res) => {
        if (res.status === 401) {
          alert("登录已过期，请重新登录！");
          this.logout();
          throw new Error("未授权");
        }
        return res.json();
      })
      .then((data) => {
        console.log("接口返回：", data);
        // 🔥 核心修复：兼容 Spring Boot 默认错误格式，显示真实原因
        if (data.code !== 200) {
          const msg = data.msg || data.message || (data.status && data.error ? `[${data.status}] ${data.error}` : "接口请求失败");
          throw new Error(msg);
        }
        return data;
      })
      .catch((err) => {
        console.error("请求异常：", err);
        // 避免重复弹窗：如果已经 alert 过（如401），不再弹
        if (err.message !== "未授权") {
          alert("请求失败：" + err.message);
        }
        throw err;
      });
  },

  checkLogin: function () {
    const token = localStorage.getItem("admin_token");
    if (!token) {
      alert("请先登录管理员账号！");
      window.location.href = "admin-login.html";
      return false;
    }
    return true;
  },

  logout: function () {
    localStorage.removeItem("admin_token");
    localStorage.removeItem("admin_name");
    window.location.href = "admin-login.html";
  },

  // 保留原方法供其他地方使用
  formatTime: function (datetimeStr) {
    if (!datetimeStr) return "";
    return datetimeStr.replace("T", " ");
  },
};

window.SeckillUtil = SeckillUtil;